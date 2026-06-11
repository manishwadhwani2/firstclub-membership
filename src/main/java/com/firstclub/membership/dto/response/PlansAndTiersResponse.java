package com.firstclub.membership.dto.response;

import com.firstclub.membership.enums.PlanType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PlansAndTiersResponse {
    private List<PlanResponse> plans;
    private List<TierResponse> tiers;

    @Data
    @Builder
    public static class PlanResponse {
        private Long id;
        private PlanType planType;
        private String name;
        private BigDecimal price;
        private Integer durationDays;
        private String description;
    }
}
