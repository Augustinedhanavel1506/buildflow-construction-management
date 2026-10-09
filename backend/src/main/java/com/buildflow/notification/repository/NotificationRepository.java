package com.buildflow.notification.repository;

import com.buildflow.auth.entity.Role;
import com.buildflow.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// A user sees notifications addressed to them personally, plus (unless they are an engineer)
// the business-wide and role-wide ones. Engineers must never see broadcast business events such
// as billing or budget alerts, so `broadcast` is false for them.
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
            select n from Notification n
            where n.business.id = :businessId
              and (n.targetUserId = :userId
                   or (n.targetUserId is null and :broadcast = true and (n.targetRole is null or n.targetRole = :role)))
            order by n.createdAt desc
            """)
    Page<Notification> findVisible(@Param("businessId") Long businessId, @Param("userId") Long userId,
                                    @Param("role") Role role, @Param("broadcast") boolean broadcast, Pageable pageable);

    @Query("""
            select count(n) from Notification n
            where n.business.id = :businessId and n.read = false
              and (n.targetUserId = :userId
                   or (n.targetUserId is null and :broadcast = true and (n.targetRole is null or n.targetRole = :role)))
            """)
    long countUnreadVisible(@Param("businessId") Long businessId, @Param("userId") Long userId,
                             @Param("role") Role role, @Param("broadcast") boolean broadcast);

    Optional<Notification> findByIdAndBusinessId(Long id, Long businessId);

    @Modifying
    @Query("""
            update Notification n set n.read = true
            where n.business.id = :businessId and n.read = false
              and (n.targetUserId = :userId
                   or (n.targetUserId is null and :broadcast = true and (n.targetRole is null or n.targetRole = :role)))
            """)
    void markAllAsRead(@Param("businessId") Long businessId, @Param("userId") Long userId,
                        @Param("role") Role role, @Param("broadcast") boolean broadcast);
}
