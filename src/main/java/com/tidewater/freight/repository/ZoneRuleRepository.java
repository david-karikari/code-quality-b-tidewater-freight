package com.tidewater.freight.repository;

import com.tidewater.freight.exception.UnknownZoneException;
import com.tidewater.freight.pricing.ZonePricingRule;

public interface ZoneRuleRepository {

    /**
     * @return the rule for the zone: never null
     * @throws UnknownZoneException if no rule exists for this zone
     */

    ZonePricingRule findRule(String zone);
}
