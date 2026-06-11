package com.firstclub.membership.dto.response;

import com.firstclub.membership.enums.BenefitType;
import com.firstclub.membership.enums.MembershipStatus;
import com.firstclub.membership.enums.PlanType;
import com.firstclub.membership.enums.TierLevel;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class UserMembershipResponse {
    private Long membershipId;
    private String userId;

    // Plan info
    private PlanType planType;
    private String planName;
    private BigDecimal planPrice;

    // Tier info
    private TierLevel tierLevel;
    private String tierName;
    private Map<BenefitType, String> tierBenefits;

    // Subscription info
    private MembershipStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean autoRenew;
    private long daysRemaining;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
