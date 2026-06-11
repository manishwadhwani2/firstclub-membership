package com.firstclub.membership.repository;

import com.firstclub.membership.entity.ActiveUserMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActiveUserMembershipRepository extends JpaRepository<ActiveUserMembership, String> {
}
