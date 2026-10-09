package com.buildflow.review.service;

import com.buildflow.auth.entity.Role;
import com.buildflow.auth.entity.User;
import com.buildflow.auth.repository.UserRepository;
import com.buildflow.boq.dto.BoqItemResponse;
import com.buildflow.boq.entity.BoqItem;
import com.buildflow.boq.entity.EstimateSource;
import com.buildflow.boq.repository.BoqItemRepository;
import com.buildflow.boq.service.BoqValidationHelper;
import com.buildflow.common.exception.BadRequestException;
import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.estimation.dto.HouseRequirementResponse;
import com.buildflow.estimation.repository.HouseRequirementRepository;
import com.buildflow.estimation.service.FloorPlanService;
import com.buildflow.notification.entity.NotificationType;
import com.buildflow.notification.service.NotificationService;
import com.buildflow.project.entity.Project;
import com.buildflow.project.repository.ProjectRepository;
import com.buildflow.review.dto.ReviewRequestCreateRequest;
import com.buildflow.review.dto.ReviewResponse;
import com.buildflow.review.dto.ReviewSubmitRequest;
import com.buildflow.review.entity.ReviewDecision;
import com.buildflow.review.entity.ReviewRequest;
import com.buildflow.review.entity.ReviewStatus;
import com.buildflow.review.repository.ReviewRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ReviewRequestService {

    private final ReviewRequestRepository reviewRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final BoqItemRepository boqItemRepository;
    private final HouseRequirementRepository houseRequirementRepository;
    private final FloorPlanService floorPlanService;
    private final BoqValidationHelper validationHelper;
    private final NotificationService notificationService;
    private final CurrentUserProvider currentUserProvider;

    public ReviewRequestService(ReviewRequestRepository reviewRepository,
                                 ProjectRepository projectRepository,
                                 UserRepository userRepository,
                                 BoqItemRepository boqItemRepository,
                                 HouseRequirementRepository houseRequirementRepository,
                                 FloorPlanService floorPlanService,
                                 BoqValidationHelper validationHelper,
                                 NotificationService notificationService,
                                 CurrentUserProvider currentUserProvider) {
        this.reviewRepository = reviewRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.boqItemRepository = boqItemRepository;
        this.houseRequirementRepository = houseRequirementRepository;
        this.floorPlanService = floorPlanService;
        this.validationHelper = validationHelper;
        this.notificationService = notificationService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public ReviewResponse create(ReviewRequestCreateRequest request) {
        User requester = currentUserProvider.getCurrentUser();
        Long businessId = requester.getBusiness().getId();

        Project project = projectRepository.findByIdAndBusinessId(request.projectId(), businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));
        User engineer = userRepository.findByIdAndBusinessId(request.engineerId(), businessId)
                .filter(u -> u.getRole() == Role.ENGINEER)
                .orElseThrow(() -> new ResourceNotFoundException("Engineer not found."));
        if (!engineer.isActive()) {
            throw new BadRequestException("This engineer account is inactive.");
        }

        boolean hasPreliminary = boqItemRepository.findByProjectIdOrderByCreatedAtAsc(project.getId()).stream()
                .anyMatch(i -> i.getEstimateSource() == EstimateSource.SYSTEM_PRELIMINARY);
        if (!hasPreliminary) {
            throw new BadRequestException("This project has no preliminary lines to review. Generate an estimate first.");
        }
        if (reviewRepository.existsByProjectIdAndStatus(project.getId(), ReviewStatus.REQUESTED)) {
            throw new BadRequestException("A review is already pending for this project.");
        }

        ReviewRequest review = new ReviewRequest();
        review.setBusiness(requester.getBusiness());
        review.setProject(project);
        review.setRequestedBy(requester);
        review.setEngineer(engineer);
        review.setMessage(request.message());
        ReviewRequest saved = reviewRepository.save(review);

        notificationService.notifyUser(requester.getBusiness(), engineer.getId(), NotificationType.REVIEW_REQUEST,
                "Review requested: " + project.getName(),
                requester.getFullName() + " asked you to review the preliminary estimate.",
                "REVIEW", saved.getId(), project.getId());
        return ReviewResponse.summary(saved);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> list() {
        User user = currentUserProvider.getCurrentUser();
        Long businessId = user.getBusiness().getId();
        List<ReviewRequest> reviews = user.getRole() == Role.ENGINEER
                ? reviewRepository.findByBusinessIdAndEngineerIdOrderByCreatedAtDesc(businessId, user.getId())
                : reviewRepository.findByBusinessIdOrderByCreatedAtDesc(businessId);
        return reviews.stream().map(ReviewResponse::summary).toList();
    }

    @Transactional(readOnly = true)
    public ReviewResponse get(Long id) {
        ReviewRequest review = findVisible(id);
        List<BoqItemResponse> items = boqItemRepository.findByProjectIdOrderByCreatedAtAsc(review.getProject().getId())
                .stream().map(BoqItemResponse::from).toList();
        HouseRequirementResponse plan = houseRequirementRepository.findByProjectId(review.getProject().getId())
                .map(HouseRequirementResponse::from).orElse(null);
        return ReviewResponse.detail(review, items, plan, floorPlanService.plansForProject(review.getProject().getId()));
    }

    @Transactional
    public ReviewResponse submit(Long id, ReviewSubmitRequest request) {
        User engineer = currentUserProvider.getCurrentUser();
        ReviewRequest review = findVisible(id);
        if (!review.getEngineer().getId().equals(engineer.getId())) {
            throw new ResourceNotFoundException("Review not found.");
        }
        requireRequested(review);

        if (request.outcome() == ReviewSubmitRequest.Outcome.REQUEST_CHANGES) {
            if (request.overallNote() == null || request.overallNote().isBlank()) {
                throw new BadRequestException("Say what needs to change when sending the estimate back.");
            }
            review.setStatus(ReviewStatus.CHANGES_REQUESTED);
        } else {
            applyValidation(review, engineer, request);
            review.setStatus(ReviewStatus.COMPLETED);
        }
        review.setOutcomeNote(request.overallNote());
        review.setCompletedAt(Instant.now());
        ReviewRequest saved = reviewRepository.save(review);

        boolean completed = saved.getStatus() == ReviewStatus.COMPLETED;
        notificationService.notifyUser(saved.getBusiness(), saved.getRequestedBy().getId(), NotificationType.REVIEW_COMPLETED,
                (completed ? "Estimate validated: " : "Changes requested: ") + saved.getProject().getName(),
                engineer.getFullName() + (completed ? " validated the estimate." : " sent the estimate back with comments."),
                "REVIEW", saved.getId(), saved.getProject().getId());
        return ReviewResponse.summary(saved);
    }

    @Transactional
    public ReviewResponse cancel(Long id) {
        ReviewRequest review = findVisible(id);
        requireRequested(review);
        review.setStatus(ReviewStatus.CANCELLED);
        review.setCompletedAt(Instant.now());
        return ReviewResponse.summary(reviewRepository.save(review));
    }

    private void applyValidation(ReviewRequest review, User engineer, ReviewSubmitRequest request) {
        Project project = review.getProject();
        Map<Long, BoqItem> preliminary = new HashMap<>();
        for (BoqItem item : boqItemRepository.findByProjectIdOrderByCreatedAtAsc(project.getId())) {
            if (item.getEstimateSource() == EstimateSource.SYSTEM_PRELIMINARY) {
                preliminary.put(item.getId(), item);
            }
        }

        String label = engineer.getFullName()
                + (engineer.getRegistrationNo() != null ? ", Reg. " + engineer.getRegistrationNo() : "");
        Set<Long> handled = new HashSet<>();

        if (request.lines() != null) {
            for (ReviewSubmitRequest.Line line : request.lines()) {
                BoqItem item = preliminary.get(line.boqItemId());
                if (item == null) {
                    throw new BadRequestException("Line " + line.boqItemId()
                            + " is not a preliminary line of this project (it may already be validated or manual).");
                }
                if (!handled.add(item.getId())) {
                    throw new BadRequestException("Line " + line.boqItemId() + " appears more than once.");
                }
                ReviewDecision decision = new ReviewDecision();
                decision.setReviewRequest(review);
                decision.setBoqItemId(item.getId());
                decision.setItemName(item.getItemName());
                decision.setOriginalQuantity(item.getQuantity());
                boolean corrected = line.quantity() != null && line.quantity().compareTo(item.getQuantity()) != 0;
                decision.setCorrectedQuantity(corrected ? line.quantity() : null);
                decision.setComment(line.comment());
                review.getDecisions().add(decision);

                String note = line.comment() != null && !line.comment().isBlank() ? line.comment() : request.overallNote();
                validationHelper.markValidated(item, label, corrected ? line.quantity() : null, note);
            }
        }
        if (Boolean.TRUE.equals(request.approveRemaining())) {
            for (BoqItem item : preliminary.values()) {
                if (handled.add(item.getId())) {
                    validationHelper.markValidated(item, label, null, request.overallNote());
                }
            }
        }
        if (handled.isEmpty()) {
            throw new BadRequestException("Nothing to validate. Approve or correct at least one line, or approve the remaining lines.");
        }

        boqItemRepository.saveAll(preliminary.values().stream().filter(i -> handled.contains(i.getId())).toList());
        validationHelper.refreshProjectEstimate(project);
    }

    private void requireRequested(ReviewRequest review) {
        if (review.getStatus() != ReviewStatus.REQUESTED) {
            throw new BadRequestException("This review is already " + review.getStatus().name().toLowerCase().replace('_', ' ') + ".");
        }
    }

    // Engineers can only open reviews assigned to them; everyone else sees their business's reviews.
    private ReviewRequest findVisible(Long id) {
        User user = currentUserProvider.getCurrentUser();
        ReviewRequest review = reviewRepository.findByIdAndBusinessId(id, user.getBusiness().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Review not found."));
        if (user.getRole() == Role.ENGINEER && !review.getEngineer().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Review not found.");
        }
        return review;
    }
}
