package com.firstclub.membership.dto.request;

import com.firstclub.membership.enums.PlanType;
import com.firstclub.membership.enums.TierLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for subscribing to a membership plan.
 */
@Data
public class SubscribeRequest {

    @NotBlank(message = "userId is required")
    private String userId;

    @NotNull(message = "planType is required")
    private PlanType planType;

    /**
     * Optional: if not provided, defaults to the lowest active tier (SILVER).
     */
    private TierLevel tierLevel;
}
