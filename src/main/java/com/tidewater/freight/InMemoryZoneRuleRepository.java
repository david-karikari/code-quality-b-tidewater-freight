package com.tidewater.freight;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryZoneRuleRepository implements ZoneRuleRepository{

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
                new FlatRateZoneRule("DOMESTIC", 5.0, 0),
                new FlatRateZoneRule("REGIONAL", 8.0, 10),
                new FlatRateZoneRule("INTERNATIONAL", 15.0, 25)));
    }

    @Override
    public ZonePricingRule findRule(String zone) {
        ZonePricingRule rule = rulesByZone.get(zone);
        if (rule == null) {
            throw new IllegalArgumentException("Unknown zone: " + zone);
        }
        return rule;
    }
}
