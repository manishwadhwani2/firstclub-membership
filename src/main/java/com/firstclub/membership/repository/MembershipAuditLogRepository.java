package com.firstclub.membership.repository;

import com.firstclub.membership.entity.MembershipAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MembershipAuditLogRepository extends JpaRepository<MembershipAuditLog, Long> {
    List<MembershipAuditLog> findByUserIdOrderByTimestampDesc(String userId);
}
