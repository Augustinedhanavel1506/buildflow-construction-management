package com.buildflow.notification.entity;

import com.buildflow.auth.entity.Role;
import com.buildflow.business.entity.Business;
import com.buildflow.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "project_id")
    private Long projectId;

    /**
     * Role this notification is intended for. Null means visible to everyone in the business.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_role")
    private Role targetRole;

    // When set, only this user sees it (engineer review requests and their outcomes).
    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;
}
