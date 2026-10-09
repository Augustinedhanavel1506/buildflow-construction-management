package com.buildflow.notification.service;

import com.buildflow.auth.entity.Role;
import com.buildflow.auth.entity.User;
import com.buildflow.business.entity.Business;
import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.notification.dto.NotificationResponse;
import com.buildflow.notification.entity.Notification;
import com.buildflow.notification.entity.NotificationType;
import com.buildflow.notification.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CurrentUserProvider currentUserProvider;

    public NotificationService(NotificationRepository notificationRepository,
                                CurrentUserProvider currentUserProvider) {
        this.notificationRepository = notificationRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(Pageable pageable) {
        User user = currentUserProvider.getCurrentUser();
        return notificationRepository.findVisible(user.getBusiness().getId(), user.getId(), user.getRole(),
                        receivesBroadcasts(user), pageable)
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        User user = currentUserProvider.getCurrentUser();
        return notificationRepository.countUnreadVisible(user.getBusiness().getId(), user.getId(), user.getRole(),
                receivesBroadcasts(user));
    }

    @Transactional
    public NotificationResponse markAsRead(Long id) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        Notification notification = notificationRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found."));
        notification.setRead(true);
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllAsRead() {
        User user = currentUserProvider.getCurrentUser();
        notificationRepository.markAllAsRead(user.getBusiness().getId(), user.getId(), user.getRole(),
                receivesBroadcasts(user));
    }

    /**
     * Creates a notification visible to everyone in the business (except engineers).
     */
    @Transactional
    public void notifyBusiness(Business business, NotificationType type, String title, String message,
                                String entityType, Long entityId, Long projectId) {
        notify(business, type, title, message, entityType, entityId, projectId, null, null);
    }

    /**
     * Creates a notification visible only to users with the given role.
     */
    @Transactional
    public void notifyRole(Business business, Role targetRole, NotificationType type, String title, String message,
                            String entityType, Long entityId, Long projectId) {
        notify(business, type, title, message, entityType, entityId, projectId, targetRole, null);
    }

    /**
     * Creates a notification visible only to one user.
     */
    @Transactional
    public void notifyUser(Business business, Long userId, NotificationType type, String title, String message,
                            String entityType, Long entityId, Long projectId) {
        notify(business, type, title, message, entityType, entityId, projectId, null, userId);
    }

    // Engineers are external reviewers, so they only see notifications addressed to them.
    private boolean receivesBroadcasts(User user) {
        return user.getRole() != Role.ENGINEER;
    }

    private void notify(Business business, NotificationType type, String title, String message,
                         String entityType, Long entityId, Long projectId, Role targetRole, Long targetUserId) {
        Notification notification = new Notification();
        notification.setBusiness(business);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setEntityType(entityType);
        notification.setEntityId(entityId);
        notification.setProjectId(projectId);
        notification.setTargetRole(targetRole);
        notification.setTargetUserId(targetUserId);
        notificationRepository.save(notification);
    }
}
