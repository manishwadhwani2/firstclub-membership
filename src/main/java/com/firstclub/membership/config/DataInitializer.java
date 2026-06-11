package com.firstclub.membership.config;

import com.firstclub.membership.entity.*;
import com.firstclub.membership.enums.*;
import com.firstclub.membership.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds the database with default plans, tiers, benefits, and criteria on startup.
 * In production, this would be replaced by Flyway/Liquibase migrations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final MembershipPlanRepository planRepository;
    private final MembershipTierRepository tierRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initializePlans();
        initializeTiersWithBenefitsAndCriteria();
        log.info("✅ DataInitializer: All membership plans, tiers, benefits, and criteria loaded successfully.");
    }

    private void initializePlans() {
        if (planRepository.count() > 0) return;

        planRepository.saveAll(List.of(
                MembershipPlan.builder()
                        .planType(PlanType.MONTHLY)
                        .name("Monthly Plan")
                        .price(new BigDecimal("99.00"))
                        .durationDays(30)
                        .description("Flexible monthly membership. Cancel anytime.")
                        .build(),

                MembershipPlan.builder()
                        .planType(PlanType.QUARTERLY)
                        .name("Quarterly Plan")
                        .price(new BigDecimal("249.00"))
                        .durationDays(90)
                        .description("Save 16% vs monthly. Best for regular shoppers.")
                        .build(),

                MembershipPlan.builder()
                        .planType(PlanType.YEARLY)
                        .name("Annual Plan")
                        .price(new BigDecimal("799.00"))
                        .durationDays(365)
                        .description("Save 33% vs monthly. Maximum value for frequent buyers.")
                        .build()
        ));

        log.info("Initialized {} membership plans", planRepository.count());
    }

    private void initializeTiersWithBenefitsAndCriteria() {
        if (tierRepository.count() > 0) return;

        // -------- SILVER (Default / Entry tier) --------
        MembershipTier silver = MembershipTier.builder()
                .tierLevel(TierLevel.SILVER)
                .name("Silver")
                .description("Entry-level membership with basic benefits.")
                .tierOrder(1)
                .build();

        silver.getBenefits().addAll(List.of(
                benefit(silver, BenefitType.FREE_DELIVERY, "false", "Free delivery on orders above ₹499"),
                benefit(silver, BenefitType.DISCOUNT_PERCENT, "5", "5% discount on selected categories"),
                benefit(silver, BenefitType.EXCLUSIVE_DEALS, "false", "No exclusive deals access"),
                benefit(silver, BenefitType.FASTER_DELIVERY_HOURS, "72", "Standard delivery: 72 hours")
        ));
        // No criteria = everyone qualifies for Silver by default

        // -------- GOLD --------
        MembershipTier gold = MembershipTier.builder()
                .tierLevel(TierLevel.GOLD)
                .name("Gold")
                .description("Enhanced membership with free delivery and exclusive deals.")
                .tierOrder(2)
                .build();

        gold.getBenefits().addAll(List.of(
                benefit(gold, BenefitType.FREE_DELIVERY, "true", "Free delivery on all eligible orders"),
                benefit(gold, BenefitType.DISCOUNT_PERCENT, "10", "10% discount on selected categories"),
                benefit(gold, BenefitType.EXCLUSIVE_DEALS, "true", "Access to exclusive member deals"),
                benefit(gold, BenefitType.EARLY_ACCESS_SALES, "true", "Early access to sales events"),
                benefit(gold, BenefitType.FASTER_DELIVERY_HOURS, "48", "Fast delivery: 48 hours")
        ));

        gold.getCriteria().addAll(List.of(
                criteria(gold, CriteriaType.ORDER_COUNT, 5.0, null,
                        "User has placed at least 5 orders"),
                criteria(gold, CriteriaType.ORDER_VALUE, 5000.0, null,
                        "User has spent at least ₹5,000 in the billing period")
        ));

        // -------- PLATINUM --------
        MembershipTier platinum = MembershipTier.builder()
                .tierLevel(TierLevel.PLATINUM)
                .name("Platinum")
                .description("Top-tier membership with all perks including priority support.")
                .tierOrder(3)
                .build();

        platinum.getBenefits().addAll(List.of(
                benefit(platinum, BenefitType.FREE_DELIVERY, "true", "Free delivery on all orders, no minimum"),
                benefit(platinum, BenefitType.DISCOUNT_PERCENT, "15", "15% discount on all categories"),
                benefit(platinum, BenefitType.EXCLUSIVE_DEALS, "true", "Access to all exclusive deals"),
                benefit(platinum, BenefitType.EARLY_ACCESS_SALES, "true", "Earliest access to sales"),
                benefit(platinum, BenefitType.PRIORITY_SUPPORT, "true", "24/7 dedicated priority support"),
                benefit(platinum, BenefitType.EXCLUSIVE_COUPONS, "true", "Monthly exclusive discount coupons"),
                benefit(platinum, BenefitType.FASTER_DELIVERY_HOURS, "24", "Express delivery: 24 hours")
        ));

        platinum.getCriteria().addAll(List.of(
                criteria(platinum, CriteriaType.ORDER_COUNT, 20.0, null,
                        "User has placed at least 20 orders"),
                criteria(platinum, CriteriaType.ORDER_VALUE, 20000.0, null,
                        "User has spent at least ₹20,000 in the billing period"),
                criteria(platinum, CriteriaType.COHORT, null, "PREMIUM",
                        "User belongs to the PREMIUM cohort")
        ));

        tierRepository.saveAll(List.of(silver, gold, platinum));
        log.info("Initialized {} membership tiers with benefits and criteria", tierRepository.count());
    }

    private TierBenefit benefit(MembershipTier tier, BenefitType type, String value, String description) {
        return TierBenefit.builder()
                .tier(tier)
                .benefitType(type)
                .benefitValue(value)
                .description(description)
                .build();
    }

    private TierCriteria criteria(MembershipTier tier, CriteriaType type,
                                   Double threshold, String cohortValue, String description) {
        return TierCriteria.builder()
                .tier(tier)
                .criteriaType(type)
                .threshold(threshold)
                .cohortValue(cohortValue)
                .description(description)
                .build();
    }
}
