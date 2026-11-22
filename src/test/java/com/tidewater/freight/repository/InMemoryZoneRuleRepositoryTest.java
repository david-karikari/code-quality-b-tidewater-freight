package com.tidewater.freight.repository;

import static com.tidewater.freight.testsupport.Amounts.assertAmountEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import com.tidewater.freight.exception.UnknownZoneException;
import com.tidewater.freight.pricing.FlatRateZoneRule;
import com.tidewater.freight.pricing.ZonePricingRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class InMemoryZoneRuleRepositoryTest {

    private final InMemoryZoneRuleRepository standard = InMemoryZoneRuleRepository.withStandardRules();

    @Test
    void returnsTheRuleRegisteredForAZone() {
        ZonePricingRule rule = new FlatRateZoneRule("DOMESTIC", BigDecimal.ONE, BigDecimal.ZERO);
        InMemoryZoneRuleRepository repository = new InMemoryZoneRuleRepository(List.of(rule));

        assertSame(rule, repository.findRule("DOMESTIC"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"DOMESTIC", "REGIONAL", "INTERNATIONAL"})
    void standardRulesCoverTheThreeCurrentZones(String zone) {
        assertEquals(zone, standard.findRule(zone).zone());
    }

    @ParameterizedTest(name = "{0}: 20 kg costs {1}")
    @CsvSource({
            "DOMESTIC,      100",   // 5 x 20
            "REGIONAL,      170",   // 8 x 20 + 10
            "INTERNATIONAL, 325"   // 15 x 20 + 25
    })
    void standardRulesCarryTheCurrentRatesAndFees(String zone, String expectedPrice) {
        assertAmountEquals(expectedPrice, standard.findRule(zone).price(new BigDecimal("20")));
    }

    @Test
    void unknownZoneIsReportedWithTheZoneThatWasAskedFor() {
        UnknownZoneException uze = assertThrows(UnknownZoneException.class, () -> standard.findRule("MOON"));

        assertEquals("MOON", uze.getZone());
        assertTrue(uze.getMessage().contains("MOON"));
    }

    @Test
    void zoneCodesAreCaseSensitive() {
        assertThrows(UnknownZoneException.class, () -> standard.findRule("domestic"));
    }

    @Test
    void aNullZoneIsUnknownNotACrash() {
        UnknownZoneException uze = assertThrows(UnknownZoneException.class, () -> standard.findRule(null));

        assertNull(uze.getZone());
    }

    @Test
    void twoRulesForTheSameZoneAreRejectedAtConstruction() {
        ZonePricingRule first = new FlatRateZoneRule("DOMESTIC", BigDecimal.ONE, BigDecimal.ZERO);
        ZonePricingRule second = new FlatRateZoneRule("DOMESTIC", BigDecimal.TEN, BigDecimal.ZERO);

        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
                () -> new InMemoryZoneRuleRepository(List.of(first, second)));

        assertTrue(iae.getMessage().contains("DOMESTIC"));
    }
}