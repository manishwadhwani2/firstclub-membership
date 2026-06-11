package com.firstclub.membership.service;

import com.firstclub.membership.entity.MembershipTier;
import com.firstclub.membership.entity.TierCriteria;
import com.firstclub.membership.repository.MembershipTierRepository;
import com.firstclub.membership.strategy.TierCriteriaStrategyFactory;
import com.firstclub.membership.strategy.TierEvaluationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Handles automatic tier evaluation logic.
 *
 * Iterates through all tiers from highest to lowest and returns
 * the first tier whose criteria the user satisfies.
 *
 * Criteria semantics: ANY single criteria being satisfied qualifies the user (OR logic).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TierEvaluationService {

    private final MembershipTierRepository tierRepository;
    private final TierCriteriaStrategyFactory strategyFactory;

    /**
     * Determines the highest tier a user qualifies for based on the evaluation context.
     *
     * @param context Runtime data about the user.
     * @return The highest qualifying MembershipTier.
     */
    public MembershipTier evaluateBestTier(TierEvaluationContext context) {
        List<MembershipTier> tiersDescending = tierRepository.findByActiveTrueOrderByTierOrderAsc()
                .stream()
                .sorted(Comparator.comparingInt(MembershipTier::getTierOrder).reversed())
                .toList();

        for (MembershipTier tier : tiersDescending) {
            List<TierCriteria> criteriaList = tier.getCriteria();

            // Tier with no criteria = default tier (e.g., SILVER) — all users qualify
            if (criteriaList == null || criteriaList.isEmpty()) {
                log.debug("Tier {} has no criteria — qualifies as default.", tier.getTierLevel());
                return tier;
            }

            // OR logic: user qualifies if ANY single criteria is satisfied
            boolean qualifies = criteriaList.stream().anyMatch(criteria -> {
                try {
                    return strategyFactory.resolve(criteria.getCriteriaType())
                            .evaluate(criteria, context);
                } catch (Exception e) {
                    log.error("Error evaluating criteria id={} for user={}: {}",
                            criteria.getId(), context.getUserId(), e.getMessage());
                    return false;
                }
            });

            if (qualifies) {
                log.info("User {} qualifies for tier: {}", context.getUserId(), tier.getTierLevel());
                return tier;
            }
        }

        // Fallback: return lowest tier (should not happen if SILVER has no criteria)
        return tierRepository.findLowestActiveTier()
                .orElseThrow(() -> new IllegalStateException("No active tiers found in database"));
    }
}
