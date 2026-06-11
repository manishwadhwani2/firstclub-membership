package com.firstclub.membership.repository;

import com.firstclub.membership.entity.MembershipTier;
import com.firstclub.membership.enums.TierLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipTierRepository extends JpaRepository<MembershipTier, Long> {

    List<MembershipTier> findByActiveTrueOrderByTierOrderAsc();

    Optional<MembershipTier> findByTierLevel(TierLevel tierLevel);

    /**
     * Find the lowest tier (default for new subscribers).
     */
    @Query("SELECT t FROM MembershipTier t WHERE t.active = true ORDER BY t.tierOrder ASC LIMIT 1")
    Optional<MembershipTier> findLowestActiveTier();
}
