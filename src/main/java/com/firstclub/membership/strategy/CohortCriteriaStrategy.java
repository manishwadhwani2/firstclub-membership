package com.firstclub.membership.strategy;

import com.firstclub.membership.entity.TierCriteria;
import com.firstclub.membership.enums.CriteriaType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Strategy for COHORT criteria.
 * User qualifies if their cohort matches the expected cohort value (case-insensitive).
 *
 * Useful for manually placing VIP users into higher tiers regardless of order activity.
 */
@Slf4j
@Component
public class CohortCriteriaStrategy implements TierCriteriaStrategy {

    @Override
    public CriteriaType getType() {
        return CriteriaType.COHORT;
    }

    @Override
    public boolean evaluate(TierCriteria criteria, TierEvaluationContext context) {
        if (criteria.getCohortValue() == null || context.getUserCohort() == null) {
            return false;
        }
        boolean result = criteria.getCohortValue().equalsIgnoreCase(context.getUserCohort());
        log.debug("COHORT check: userCohort='{}' matches '{}'? => {}",
                context.getUserCohort(), criteria.getCohortValue(), result);
        return result;
    }
}
