package com.firstclub.membership.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

/**
 * Shadow table to enforce the "One ACTIVE membership per user" rule
 * at the database level across multiple application instances.
 *
 * Why this exists:
 * H2 (and some older databases) do not support Partial Unique Indexes
 * (e.g., UNIQUE(user_id) WHERE status='ACTIVE').
 *
 * To prevent a race condition where two instances bypass the pessimistic lock
 * and both create an ACTIVE membership, we insert the userId into this table
 * when they subscribe, and delete the row when they cancel/expire.
 * The @Id constraint ensures the database physically rejects a second active membership.
 */
@Entity
@Table(name = "active_user_memberships")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActiveUserMembership {

    @Id
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "membership_id", nullable = false)
    private Long membershipId;
}
