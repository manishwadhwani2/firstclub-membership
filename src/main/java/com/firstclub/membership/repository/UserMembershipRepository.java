package com.firstclub.membership.repository;

import com.firstclub.membership.entity.UserMembership;
import com.firstclub.membership.enums.MembershipStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserMembershipRepository extends JpaRepository<UserMembership, Long> {

    /**
     * Finds the active membership for a user.
     * No locking — safe for read-only operations.
     */
    Optional<UserMembership> findByUserIdAndStatus(String userId, MembershipStatus status);

    /**
     * Finds and LOCKS the active membership row for a user.
     * Use this before any write operations to prevent concurrent modifications.
     * Implements Pessimistic Locking at the DB level.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT um FROM UserMembership um WHERE um.userId = :userId AND um.status = :status")
    Optional<UserMembership> findByUserIdAndStatusWithLock(
            @Param("userId") String userId,
            @Param("status") MembershipStatus status);

    boolean existsByUserIdAndStatus(String userId, MembershipStatus status);

    /**
     * Used by the expiry scheduler to find all subscriptions that have passed their end date.
     */
    @Query("SELECT um FROM UserMembership um WHERE um.status = 'ACTIVE' AND um.endDate < :today")
    List<UserMembership> findExpiredSubscriptions(@Param("today") LocalDate today);
}
