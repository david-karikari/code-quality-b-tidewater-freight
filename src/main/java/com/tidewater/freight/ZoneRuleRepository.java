package com.tidewater.freight;

public interface ZoneRuleRepository {

    /**
     * @return the rule for the zone: never null
     * @throws UnknownZoneException if no rule exists for this zone
     */

    ZonePricingRule findRule(String zone);
}
