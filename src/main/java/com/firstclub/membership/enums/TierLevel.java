package com.firstclub.membership.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Tier levels in ascending order of privilege.
 * tierOrder is used to compare tiers (higher = more privileged).
 */
@Getter
@RequiredArgsConstructor
public enum TierLevel {
    SILVER("Silver", 1),
    GOLD("Gold", 2),
    PLATINUM("Platinum", 3);

    private final String displayName;
    private final int tierOrder;

    public boolean isHigherThan(TierLevel other) {
        return this.tierOrder > other.tierOrder;
    }

    public boolean isLowerThan(TierLevel other) {
        return this.tierOrder < other.tierOrder;
    }
}
