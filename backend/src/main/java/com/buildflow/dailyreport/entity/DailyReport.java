package com.buildflow.dailyreport.entity;

import com.buildflow.auth.entity.User;
import com.buildflow.common.entity.BaseEntity;
import com.buildflow.project.entity.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "daily_reports")
public class DailyReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "workers_present")
    private Integer workersPresent;

    @Column(name = "work_completed", length = 2000)
    private String workCompleted;

    @Column(name = "materials_used", length = 2000)
    private String materialsUsed;

    @Column(length = 2000)
    private String issues;

    @Column(length = 2000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by_user_id", nullable = false)
    private User submittedBy;
}
