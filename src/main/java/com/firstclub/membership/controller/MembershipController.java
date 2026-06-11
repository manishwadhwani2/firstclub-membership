package com.firstclub.membership.controller;

import com.firstclub.membership.dto.request.SubscribeRequest;
import com.firstclub.membership.dto.request.TierChangeRequest;
import com.firstclub.membership.dto.request.TierEvaluationRequest;
import com.firstclub.membership.dto.response.ApiResponse;
import com.firstclub.membership.dto.response.PlansAndTiersResponse;
import com.firstclub.membership.dto.response.UserMembershipResponse;
import com.firstclub.membership.service.MembershipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/membership")
@RequiredArgsConstructor
@Tag(name = "Membership API", description = "FirstClub Membership Program — Plans, Tiers, and Subscriptions")
public class MembershipController {

    private final MembershipService membershipService;

    // -----------------------------------------------------------------------
    // CATALOG
    // -----------------------------------------------------------------------

    @GetMapping("/plans")
    @Operation(summary = "Get all membership plans and tiers",
               description = "Returns all active plans (Monthly/Quarterly/Yearly) and tiers (Silver/Gold/Platinum) with configurable benefits")
    public ResponseEntity<ApiResponse<PlansAndTiersResponse>> getPlansAndTiers() {
        PlansAndTiersResponse response = membershipService.getPlansAndTiers();
        return ResponseEntity.ok(ApiResponse.success(response, "Plans and tiers retrieved successfully"));
    }

    // -----------------------------------------------------------------------
    // SUBSCRIBE
    // -----------------------------------------------------------------------

    @PostMapping("/subscribe")
    @Operation(summary = "Subscribe to a membership plan",
               description = "Subscribes a user to a plan + tier. Defaults to SILVER tier if not specified.")
    public ResponseEntity<ApiResponse<UserMembershipResponse>> subscribe(
            @Valid @RequestBody SubscribeRequest request) {
        UserMembershipResponse response = membershipService.subscribe(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Membership subscribed successfully"));
    }

    // -----------------------------------------------------------------------
    // STATUS
    // -----------------------------------------------------------------------

    @GetMapping("/status/{userId}")
    @Operation(summary = "Get current membership status",
               description = "Returns the current active membership details, tier, benefits, and expiry for a user")
    public ResponseEntity<ApiResponse<UserMembershipResponse>> getMembershipStatus(
            @PathVariable String userId) {
        UserMembershipResponse response = membershipService.getMembershipStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Membership status retrieved"));
    }

    // -----------------------------------------------------------------------
    // UPGRADE / DOWNGRADE
    // -----------------------------------------------------------------------

    @PutMapping("/upgrade")
    @Operation(summary = "Upgrade membership tier",
               description = "Manually upgrades the user's tier to a higher tier (e.g., SILVER → GOLD)")
    public ResponseEntity<ApiResponse<UserMembershipResponse>> upgradeTier(
            @Valid @RequestBody TierChangeRequest request) {
        UserMembershipResponse response = membershipService.upgradeTier(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Tier upgraded successfully"));
    }

    @PutMapping("/downgrade")
    @Operation(summary = "Downgrade membership tier",
               description = "Manually downgrades the user's tier to a lower tier (e.g., GOLD → SILVER)")
    public ResponseEntity<ApiResponse<UserMembershipResponse>> downgradeTier(
            @Valid @RequestBody TierChangeRequest request) {
        UserMembershipResponse response = membershipService.downgradeTier(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Tier downgraded successfully"));
    }

    // -----------------------------------------------------------------------
    // CANCEL
    // -----------------------------------------------------------------------

    @PutMapping("/cancel/{userId}")
    @Operation(summary = "Cancel membership",
               description = "Cancels the user's active membership subscription")
    public ResponseEntity<ApiResponse<UserMembershipResponse>> cancelMembership(
            @PathVariable String userId) {
        UserMembershipResponse response = membershipService.cancelMembership(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Membership cancelled successfully"));
    }

    // -----------------------------------------------------------------------
    // AUTO TIER EVALUATION
    // -----------------------------------------------------------------------

    @PostMapping("/evaluate-tier")
    @Operation(summary = "Auto-evaluate and update tier",
               description = "Evaluates the user's order activity and automatically promotes/demotes to the appropriate tier")
    public ResponseEntity<ApiResponse<UserMembershipResponse>> evaluateTier(
            @Valid @RequestBody TierEvaluationRequest request) {
        UserMembershipResponse response = membershipService.evaluateAndUpdateTier(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Tier evaluation completed"));
    }

    // -----------------------------------------------------------------------
    // AUDIT HISTORY
    // -----------------------------------------------------------------------

    @GetMapping("/history/{userId}")
    @Operation(summary = "Get membership history",
               description = "Returns the full audit trail of all membership actions for a user")
    public ResponseEntity<ApiResponse<List<String>>> getMembershipHistory(
            @PathVariable String userId) {
        List<String> history = membershipService.getMembershipHistory(userId);
        return ResponseEntity.ok(ApiResponse.success(history, "Membership history retrieved"));
    }
}
