package com.firstclub.membership.dto.request;

import com.firstclub.membership.enums.TierLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for manually upgrading or downgrading a user's tier.
 */
@Data
public class TierChangeRequest {

    @NotBlank(message = "userId is required")
    private String userId;

    @NotNull(message = "targetTier is required")
    private TierLevel targetTier;

    private String reason;
}
