package com.firstclub.membership.strategy;

import com.firstclub.membership.entity.TierCriteria;
import com.firstclub.membership.enums.CriteriaType;

/**
 * Strategy interface for evaluating tier promotion criteria.
 *
 * Each implementation evaluates ONE type of criteria (ORDER_COUNT, ORDER_VALUE, COHORT).
 * Adding a new criteria type only requires:
 *   1. Adding a new CriteriaType enum value
 *   2. Creating a new implementation of this interface
 *   3. Registering it in TierCriteriaStrategyFactory
 *
 * No changes to TierEvaluationService or any entity are needed.
 */
public interface TierCriteriaStrategy {

    /**
     * @return The CriteriaType this strategy handles.
     */
    CriteriaType getType();

    /**
     * Evaluates whether the user meets the given criteria.
     *
     * @param criteria  The criteria configuration from the database.
     * @param context   Runtime data about the user (order count, order value, cohort).
     * @return true if the user satisfies this criteria.
     */
    boolean evaluate(TierCriteria criteria, TierEvaluationContext context);
}
