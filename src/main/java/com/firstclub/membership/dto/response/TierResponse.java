package com.firstclub.membership.dto.response;

import com.firstclub.membership.enums.BenefitType;
import com.firstclub.membership.enums.TierLevel;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class TierResponse {
    private Long id;
    private TierLevel tierLevel;
    private String name;
    private String description;
    private Integer tierOrder;
    private Map<BenefitType, String> benefits;
    private List<CriteriaResponse> criteria;

    @Data
    @Builder
    public static class CriteriaResponse {
        private String criteriaType;
        private Double threshold;
        private String cohortValue;
        private String description;
    }
}
