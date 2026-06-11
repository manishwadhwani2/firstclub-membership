package com.firstclub.membership.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Configures ShedLock for distributed scheduler locking.
 *
 * Problem solved:
 *   Without this, @Scheduled jobs (e.g., processExpiredMemberships) run
 *   on EVERY instance simultaneously when scaled horizontally.
 *   For N=3 instances → same 100 expired memberships processed 3 times.
 *
 * How ShedLock works:
 *   1. Before running a @Scheduled job, the instance tries to INSERT a row
 *      into the `shedlock` table with the job name as the primary key.
 *   2. Only ONE instance succeeds (PK constraint). That instance runs the job.
 *   3. Other instances get a duplicate key error → they skip the job.
 *   4. After the job finishes (or lockAtMostFor elapses), the lock is released.
 *
 * In production:
 *   Replace JdbcTemplateLockProvider with RedisLockProvider for better performance
 *   and to avoid DB load from lock operations.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class ShedLockConfig {

    /**
     * Uses JDBC (same DB as the app) as the lock storage.
     * Requires the `shedlock` table to exist (created in data.sql).
     *
     * usingDbTime() ensures lock timestamps use DB server time,
     * avoiding issues when app servers have different system clocks.
     */
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime()
                        .build()
        );
    }
}
