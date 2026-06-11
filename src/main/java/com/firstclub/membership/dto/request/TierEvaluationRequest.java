package com.firstclub.membership.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for triggering automatic tier evaluation for a user.
 * Simulates receiving order data from an external Order Service.
 */
@Data
public class TierEvaluationRequest {

    @NotBlank(message = "userId is required")
    private String userId;

    @Min(value = 0, message = "orderCount must be >= 0")
    private int orderCount;

    @Min(value = 0, message = "totalOrderValue must be >= 0")
    private double totalOrderValue;

    /**
     * Optional cohort identifier (e.g., "PREMIUM", "VIP").
     */
    private String userCohort;
}
