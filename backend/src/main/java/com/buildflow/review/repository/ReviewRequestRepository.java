package com.buildflow.review.repository;

import com.buildflow.review.entity.ReviewRequest;
import com.buildflow.review.entity.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRequestRepository extends JpaRepository<ReviewRequest, Long> {
    List<ReviewRequest> findByBusinessIdOrderByCreatedAtDesc(Long businessId);

    List<ReviewRequest> findByBusinessIdAndEngineerIdOrderByCreatedAtDesc(Long businessId, Long engineerId);

    Optional<ReviewRequest> findByIdAndBusinessId(Long id, Long businessId);

    boolean existsByProjectIdAndStatus(Long projectId, ReviewStatus status);
}
