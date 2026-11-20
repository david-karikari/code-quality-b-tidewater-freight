package com.tidewater.freight.repository;

import com.tidewater.freight.exception.UnknownZoneException;
import com.tidewater.freight.pricing.FlatRateZoneRule;
import com.tidewater.freight.pricing.ZonePricingRule;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Holds zone rules in memory, indexed by zone code. */
public final class InMemoryZoneRuleRepository implements ZoneRuleRepository {

    private final Map<String, ZonePricingRule> rulesByZone = new HashMap<>();

    /** @throws IllegalArgumentException if two rules share a zone code */
    public InMemoryZoneRuleRepository(Collection<? extends ZonePricingRule> rules) {
        for (ZonePricingRule rule : rules) {
            if (rulesByZone.putIfAbsent(rule.zone(), rule) != null) {
                throw new IllegalArgumentException("Duplicate rule for zone: " + rule.zone());
            }
        }
    }

    /** Tidewater's current zones. Adding a zone means adding one rule to this list. */
    public static InMemoryZoneRuleRepository withStandardRules() {
        return new InMemoryZoneRuleRepository(List.of(
                new FlatRateZoneRule("DOMESTIC", new BigDecimal("5"), BigDecimal.ZERO),
                new FlatRateZoneRule("REGIONAL", new BigDecimal("8"), new BigDecimal("10")),
                new FlatRateZoneRule("INTERNATIONAL", new BigDecimal("15"), new BigDecimal("25"))));
    }

    @Override
    public ZonePricingRule findRule(String zone) {
        ZonePricingRule rule = rulesByZone.get(zone);
        if (rule == null) {
            throw new UnknownZoneException(zone);
        }
        return rule;
    }
}
