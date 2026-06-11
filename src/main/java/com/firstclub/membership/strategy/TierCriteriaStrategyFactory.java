package com.firstclub.membership.strategy;

import com.firstclub.membership.enums.CriteriaType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for resolving the correct TierCriteriaStrategy by CriteriaType.
 *
 * Spring auto-injects all TierCriteriaStrategy implementations via constructor injection.
 * To add a new strategy: create an @Component implementing TierCriteriaStrategy — no factory code changes needed.
 *
 * This is the Factory pattern combined with Open/Closed Principle.
 */
@Slf4j
@Component
public class TierCriteriaStrategyFactory {

    private final Map<CriteriaType, TierCriteriaStrategy> strategyMap;

    public TierCriteriaStrategyFactory(List<TierCriteriaStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(TierCriteriaStrategy::getType, Function.identity()));
        log.info("Registered {} tier criteria strategies: {}", strategies.size(), strategyMap.keySet());
    }

    /**
     * Resolves the strategy for the given criteria type.
     *
     * @param type The CriteriaType to look up.
     * @return The matching strategy.
     * @throws IllegalArgumentException if no strategy is registered for the type.
     */
    public TierCriteriaStrategy resolve(CriteriaType type) {
        TierCriteriaStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException(
                    "No TierCriteriaStrategy registered for type: " + type +
                    ". Please implement and register a new strategy.");
        }
        return strategy;
    }
}
