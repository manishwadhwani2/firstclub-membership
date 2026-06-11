package com.firstclub.membership.strategy;

import com.firstclub.membership.entity.TierCriteria;
import com.firstclub.membership.enums.CriteriaType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Strategy for ORDER_COUNT criteria.
 * User qualifies if their total order count >= the threshold in criteria.
 */
@Slf4j
@Component
public class OrderCountCriteriaStrategy implements TierCriteriaStrategy {

    @Override
    public CriteriaType getType() {
        return CriteriaType.ORDER_COUNT;
    }

    @Override
    public boolean evaluate(TierCriteria criteria, TierEvaluationContext context) {
        if (criteria.getThreshold() == null) {
            log.warn("ORDER_COUNT criteria id={} has no threshold set. Defaulting to false.", criteria.getId());
            return false;
        }
        boolean result = context.getOrderCount() >= criteria.getThreshold();
        log.debug("ORDER_COUNT check: userOrders={} >= threshold={} => {}",
                context.getOrderCount(), criteria.getThreshold(), result);
        return result;
    }
}
