package com.firstclub.membership.strategy;

import com.firstclub.membership.entity.TierCriteria;
import com.firstclub.membership.enums.CriteriaType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Strategy for ORDER_VALUE criteria.
 * User qualifies if their total order value >= the threshold in criteria.
 */
@Slf4j
@Component
public class OrderValueCriteriaStrategy implements TierCriteriaStrategy {

    @Override
    public CriteriaType getType() {
        return CriteriaType.ORDER_VALUE;
    }

    @Override
    public boolean evaluate(TierCriteria criteria, TierEvaluationContext context) {
        if (criteria.getThreshold() == null) {
            log.warn("ORDER_VALUE criteria id={} has no threshold set. Defaulting to false.", criteria.getId());
            return false;
        }
        boolean result = context.getTotalOrderValue() >= criteria.getThreshold();
        log.debug("ORDER_VALUE check: userValue={} >= threshold={} => {}",
                context.getTotalOrderValue(), criteria.getThreshold(), result);
        return result;
    }
}
