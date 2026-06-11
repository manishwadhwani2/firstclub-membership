package com.firstclub.membership.service;

import com.firstclub.membership.dto.request.SubscribeRequest;
import com.firstclub.membership.dto.request.TierChangeRequest;
import com.firstclub.membership.dto.request.TierEvaluationRequest;
import com.firstclub.membership.dto.response.PlansAndTiersResponse;
import com.firstclub.membership.dto.response.TierResponse;
import com.firstclub.membership.dto.response.UserMembershipResponse;
import com.firstclub.membership.entity.*;
import com.firstclub.membership.enums.BenefitType;
import com.firstclub.membership.enums.MembershipStatus;
import com.firstclub.membership.exception.MembershipException;
import com.firstclub.membership.exception.ResourceNotFoundException;
import com.firstclub.membership.repository.*;
import com.firstclub.membership.strategy.TierEvaluationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipServiceImpl implements MembershipService {

    private final MembershipPlanRepository planRepository;
    private final MembershipTierRepository tierRepository;
    private final UserMembershipRepository membershipRepository;
    private final ActiveUserMembershipRepository activeUserMembershipRepository;
    private final MembershipAuditLogRepository auditLogRepository;
    private final TierEvaluationService tierEvaluationService;

    // -----------------------------------------------------------------------
    // CATALOG
    // -----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PlansAndTiersResponse getPlansAndTiers() {
        List<PlansAndTiersResponse.PlanResponse> plans = planRepository.findByActiveTrue()
                .stream()
                .map(plan -> PlansAndTiersResponse.PlanResponse.builder()
                        .id(plan.getId())
                        .planType(plan.getPlanType())
                        .name(plan.getName())
                        .price(plan.getPrice())
                        .durationDays(plan.getDurationDays())
                        .description(plan.getDescription())
                        .build())
                .toList();

        List<TierResponse> tiers = tierRepository.findByActiveTrueOrderByTierOrderAsc()
                .stream()
                .map(this::mapToTierResponse)
                .toList();

        return PlansAndTiersResponse.builder()
                .plans(plans)
                .tiers(tiers)
                .build();
    }

    // -----------------------------------------------------------------------
    // SUBSCRIBE
    // -----------------------------------------------------------------------

    @Override
    @Transactional
    public UserMembershipResponse subscribe(SubscribeRequest request) {
        String userId = request.getUserId();

        // Guard: check for existing active membership (pessimistic lock)
        membershipRepository.findByUserIdAndStatusWithLock(userId, MembershipStatus.ACTIVE)
                .ifPresent(existing -> {
                    throw new MembershipException(
                            "User " + userId + " already has an active membership. " +
                            "Please cancel before subscribing to a new plan.");
                });

        MembershipPlan plan = planRepository.findByPlanTypeAndActiveTrue(request.getPlanType())
                .orElseThrow(() -> new ResourceNotFoundException("MembershipPlan", request.getPlanType().name()));

        // Default to lowest tier (SILVER) if not specified
        MembershipTier tier;
        if (request.getTierLevel() != null) {
            tier = tierRepository.findByTierLevel(request.getTierLevel())
                    .orElseThrow(() -> new ResourceNotFoundException("MembershipTier", request.getTierLevel().name()));
        } else {
            tier = tierRepository.findLowestActiveTier()
                    .orElseThrow(() -> new IllegalStateException("No active tiers found"));
        }

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(plan.getDurationDays());

        UserMembership membership = UserMembership.builder()
                .userId(userId)
                .plan(plan)
                .tier(tier)
                .status(MembershipStatus.ACTIVE)
                .startDate(startDate)
                .endDate(endDate)
                .autoRenew(true)
                .build();

        UserMembership saved = membershipRepository.save(membership);

        // Enforce DB-level uniqueness (works across instances, replaces partial index)
        activeUserMembershipRepository.save(
                ActiveUserMembership.builder()
                        .userId(userId)
                        .membershipId(saved.getId())
                        .build()
        );

        auditLog(userId, "SUBSCRIBED", null,
                String.format("plan=%s, tier=%s, expiry=%s", plan.getPlanType(), tier.getTierLevel(), endDate));

        log.info("User {} subscribed to plan={}, tier={}", userId, plan.getPlanType(), tier.getTierLevel());
        return mapToResponse(saved);
    }

    // -----------------------------------------------------------------------
    // GET STATUS
    // -----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public UserMembershipResponse getMembershipStatus(String userId) {
        UserMembership membership = getActiveMembership(userId);
        return mapToResponse(membership);
    }

    // -----------------------------------------------------------------------
    // UPGRADE TIER
    // -----------------------------------------------------------------------

    @Override
    @Transactional
    public UserMembershipResponse upgradeTier(TierChangeRequest request) {
        UserMembership membership = getActiveMembershipWithLock(request.getUserId());
        MembershipTier currentTier = membership.getTier();

        MembershipTier targetTier = tierRepository.findByTierLevel(request.getTargetTier())
                .orElseThrow(() -> new ResourceNotFoundException("MembershipTier", request.getTargetTier().name()));

        if (!request.getTargetTier().isHigherThan(currentTier.getTierLevel())) {
            throw new MembershipException(
                    String.format("Cannot upgrade: target tier %s is not higher than current tier %s",
                            request.getTargetTier(), currentTier.getTierLevel()));
        }

        String previous = currentTier.getTierLevel().name();
        membership.setTier(targetTier);
        UserMembership saved = membershipRepository.save(membership);

        auditLog(request.getUserId(), "UPGRADED",
                "tier=" + previous,
                "tier=" + targetTier.getTierLevel() + ", reason=" + request.getReason());

        log.info("User {} upgraded: {} → {}", request.getUserId(), previous, targetTier.getTierLevel());
        return mapToResponse(saved);
    }

    // -----------------------------------------------------------------------
    // DOWNGRADE TIER
    // -----------------------------------------------------------------------

    @Override
    @Transactional
    public UserMembershipResponse downgradeTier(TierChangeRequest request) {
        UserMembership membership = getActiveMembershipWithLock(request.getUserId());
        MembershipTier currentTier = membership.getTier();

        MembershipTier targetTier = tierRepository.findByTierLevel(request.getTargetTier())
                .orElseThrow(() -> new ResourceNotFoundException("MembershipTier", request.getTargetTier().name()));

        if (!request.getTargetTier().isLowerThan(currentTier.getTierLevel())) {
            throw new MembershipException(
                    String.format("Cannot downgrade: target tier %s is not lower than current tier %s",
                            request.getTargetTier(), currentTier.getTierLevel()));
        }

        String previous = currentTier.getTierLevel().name();
        membership.setTier(targetTier);
        UserMembership saved = membershipRepository.save(membership);

        auditLog(request.getUserId(), "DOWNGRADED",
                "tier=" + previous,
                "tier=" + targetTier.getTierLevel() + ", reason=" + request.getReason());

        log.info("User {} downgraded: {} → {}", request.getUserId(), previous, targetTier.getTierLevel());
        return mapToResponse(saved);
    }

    // -----------------------------------------------------------------------
    // CANCEL
    // -----------------------------------------------------------------------

    @Override
    @Transactional
    public UserMembershipResponse cancelMembership(String userId) {
        UserMembership membership = getActiveMembershipWithLock(userId);
        String previousState = String.format("plan=%s, tier=%s, status=ACTIVE",
                membership.getPlan().getPlanType(), membership.getTier().getTierLevel());

        membership.setStatus(MembershipStatus.CANCELLED);
        UserMembership saved = membershipRepository.save(membership);

        // Remove DB uniqueness constraint lock
        activeUserMembershipRepository.deleteById(userId);

        auditLog(userId, "CANCELLED", previousState, "status=CANCELLED");
        log.info("User {} cancelled their membership.", userId);
        return mapToResponse(saved);
    }

    // -----------------------------------------------------------------------
    // AUTO TIER EVALUATION
    // -----------------------------------------------------------------------

    @Override
    @Transactional
    public UserMembershipResponse evaluateAndUpdateTier(TierEvaluationRequest request) {
        UserMembership membership = getActiveMembershipWithLock(request.getUserId());

        TierEvaluationContext context = TierEvaluationContext.builder()
                .userId(request.getUserId())
                .orderCount(request.getOrderCount())
                .totalOrderValue(request.getTotalOrderValue())
                .userCohort(request.getUserCohort())
                .build();

        MembershipTier bestTier = tierEvaluationService.evaluateBestTier(context);
        MembershipTier currentTier = membership.getTier();

        if (!bestTier.getTierLevel().equals(currentTier.getTierLevel())) {
            String previous = currentTier.getTierLevel().name();
            membership.setTier(bestTier);
            membershipRepository.save(membership);

            String action = bestTier.getTierLevel().isHigherThan(currentTier.getTierLevel())
                    ? "TIER_AUTO_UPGRADED" : "TIER_AUTO_DOWNGRADED";

            auditLog(request.getUserId(), action,
                    "tier=" + previous,
                    String.format("tier=%s, orderCount=%d, orderValue=%.2f",
                            bestTier.getTierLevel(), request.getOrderCount(), request.getTotalOrderValue()));

            log.info("User {} auto-evaluated: {} → {}", request.getUserId(), previous, bestTier.getTierLevel());
        } else {
            log.info("User {} tier unchanged after evaluation: {}", request.getUserId(), currentTier.getTierLevel());
        }

        return mapToResponse(membershipRepository.findByUserIdAndStatus(
                request.getUserId(), MembershipStatus.ACTIVE).orElseThrow());
    }

    // -----------------------------------------------------------------------
    // AUDIT HISTORY
    // -----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<String> getMembershipHistory(String userId) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId)
                .stream()
                .map(log -> String.format("[%s] %s | %s → %s | %s",
                        log.getTimestamp(), log.getAction(),
                        log.getPreviousState(), log.getNewState(), log.getRemarks()))
                .toList();
    }

    // -----------------------------------------------------------------------
    // SCHEDULED: Expire memberships
    // -----------------------------------------------------------------------

    /**
     * Runs daily at midnight to mark expired memberships as EXPIRED.
     *
     * FIX — Multi-instance safety:
     * @SchedulerLock ensures only ONE instance runs this job at a time.
     * Without it, all N instances would run simultaneously at midnight,
     * processing the same expired memberships N times each.
     *
     * lockAtLeastFor  = PT1M  → hold lock for at least 1 min (prevents rapid re-execution)
     * lockAtMostFor   = PT10M → force-release after 10 min even if instance crashes mid-job
     *
     * In production: replace JdbcTemplateLockProvider with RedisLockProvider
     * for lower DB overhead on lock operations.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @SchedulerLock(
            name = "processExpiredMemberships",
            lockAtLeastFor = "PT1M",
            lockAtMostFor  = "PT10M"
    )
    @Transactional
    public void processExpiredMemberships() {
        List<UserMembership> expired = membershipRepository.findExpiredSubscriptions(LocalDate.now());
        log.info("Processing {} expired memberships", expired.size());

        for (UserMembership membership : expired) {
            String previous = String.format("status=ACTIVE, expiry=%s", membership.getEndDate());
            membership.setStatus(MembershipStatus.EXPIRED);
            membershipRepository.save(membership);
            
            // Remove DB uniqueness constraint lock
            activeUserMembershipRepository.deleteById(membership.getUserId());
            
            auditLog(membership.getUserId(), "EXPIRED", previous, "status=EXPIRED");
        }
    }

    // -----------------------------------------------------------------------
    // HELPERS
    // -----------------------------------------------------------------------

    private UserMembership getActiveMembership(String userId) {
        return membershipRepository.findByUserIdAndStatus(userId, MembershipStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active membership", "userId=" + userId));
    }

    private UserMembership getActiveMembershipWithLock(String userId) {
        return membershipRepository.findByUserIdAndStatusWithLock(userId, MembershipStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active membership", "userId=" + userId));
    }

    private UserMembershipResponse mapToResponse(UserMembership membership) {
        Map<BenefitType, String> benefits = membership.getTier().getBenefits().stream()
                .collect(Collectors.toMap(
                        TierBenefit::getBenefitType,
                        TierBenefit::getBenefitValue));

        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), membership.getEndDate());

        return UserMembershipResponse.builder()
                .membershipId(membership.getId())
                .userId(membership.getUserId())
                .planType(membership.getPlan().getPlanType())
                .planName(membership.getPlan().getName())
                .planPrice(membership.getPlan().getPrice())
                .tierLevel(membership.getTier().getTierLevel())
                .tierName(membership.getTier().getName())
                .tierBenefits(benefits)
                .status(membership.getStatus())
                .startDate(membership.getStartDate())
                .endDate(membership.getEndDate())
                .autoRenew(membership.isAutoRenew())
                .daysRemaining(Math.max(0, daysRemaining))
                .createdAt(membership.getCreatedAt())
                .updatedAt(membership.getUpdatedAt())
                .build();
    }

    private TierResponse mapToTierResponse(MembershipTier tier) {
        Map<BenefitType, String> benefits = tier.getBenefits().stream()
                .collect(Collectors.toMap(TierBenefit::getBenefitType, TierBenefit::getBenefitValue));

        List<TierResponse.CriteriaResponse> criteriaResponses = tier.getCriteria().stream()
                .map(c -> TierResponse.CriteriaResponse.builder()
                        .criteriaType(c.getCriteriaType().name())
                        .threshold(c.getThreshold())
                        .cohortValue(c.getCohortValue())
                        .description(c.getDescription())
                        .build())
                .toList();

        return TierResponse.builder()
                .id(tier.getId())
                .tierLevel(tier.getTierLevel())
                .name(tier.getName())
                .description(tier.getDescription())
                .tierOrder(tier.getTierOrder())
                .benefits(benefits)
                .criteria(criteriaResponses)
                .build();
    }

    private void auditLog(String userId, String action, String previousState, String newState) {
        auditLogRepository.save(MembershipAuditLog.builder()
                .userId(userId)
                .action(action)
                .previousState(previousState)
                .newState(newState)
                .build());
    }
}
