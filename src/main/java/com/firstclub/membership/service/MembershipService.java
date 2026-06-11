package com.firstclub.membership.service;

import com.firstclub.membership.dto.request.SubscribeRequest;
import com.firstclub.membership.dto.request.TierChangeRequest;
import com.firstclub.membership.dto.request.TierEvaluationRequest;
import com.firstclub.membership.dto.response.PlansAndTiersResponse;
import com.firstclub.membership.dto.response.UserMembershipResponse;

import java.util.List;

/**
 * Core membership service contract.
 * All business operations are defined here for clean abstraction.
 */
public interface MembershipService {

    /**
     * Retrieves all active membership plans and tiers with their benefits.
     * This is the catalog endpoint for the shopping journey.
     */
    PlansAndTiersResponse getPlansAndTiers();

    /**
     * Subscribes a user to a membership plan and tier.
     * Throws MembershipException if user already has an active membership.
     * Concurrency-safe via pessimistic locking.
     */
    UserMembershipResponse subscribe(SubscribeRequest request);

    /**
     * Retrieves the current membership status for a user.
     */
    UserMembershipResponse getMembershipStatus(String userId);

    /**
     * Manually upgrades a user's tier to a higher tier.
     * Validates that targetTier is strictly higher than current tier.
     */
    UserMembershipResponse upgradeTier(TierChangeRequest request);

    /**
     * Manually downgrades a user's tier to a lower tier.
     * Validates that targetTier is strictly lower than current tier.
     */
    UserMembershipResponse downgradeTier(TierChangeRequest request);

    /**
     * Cancels a user's active membership.
     */
    UserMembershipResponse cancelMembership(String userId);

    /**
     * Evaluates and automatically adjusts a user's tier based on order activity.
     * Finds the highest tier the user qualifies for and promotes/demotes accordingly.
     */
    UserMembershipResponse evaluateAndUpdateTier(TierEvaluationRequest request);

    /**
     * Returns the full audit log for a user's membership history.
     */
    List<String> getMembershipHistory(String userId);
}
