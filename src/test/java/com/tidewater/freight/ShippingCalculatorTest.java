package com.tidewater.freight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShippingCalculatorTest {

    private static final BigDecimal TEN_KG = new BigDecimal("10");

    @Mock
    private ZoneRuleRepository zoneRules;
    @Mock
    private ZonePricingRule rule;
    @Mock
    private HeavyItemSurcharge heavyItemSurcharge;

    private ShippingCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ShippingCalculator(zoneRules, heavyItemSurcharge);
    }

    private Shipment shipment(String zone, ServiceLevel level) {
        return new Shipment(zone, TEN_KG, level);
    }

    @Test
    void addsTheSurchargeBeforeApplyingTheServiceLevel() {
        when(zoneRules.findRule("DOMESTIC")).thenReturn(rule);
        when(rule.price(TEN_KG)).thenReturn(new BigDecimal("100"));
        when(heavyItemSurcharge.forWeight(TEN_KG)).thenReturn(new BigDecimal("20"));

        BigDecimal cost = calculator.calculateCost(shipment("DOMESTIC", ServiceLevel.EXPRESS));

        // (100 + 20) x 1.5 = 180. Applying express first would give 170.
        assertEquals(new BigDecimal("180.00"), cost);
    }

    @Test
    void returnsTheZonePriceToTwoDecimalPlacesWhenNothingElseApplies() {
        when(zoneRules.findRule("DOMESTIC")).thenReturn(rule);
        when(rule.price(TEN_KG)).thenReturn(new BigDecimal("50"));
        when(heavyItemSurcharge.forWeight(TEN_KG)).thenReturn(BigDecimal.ZERO);

        BigDecimal cost = calculator.calculateCost(shipment("DOMESTIC", ServiceLevel.STANDARD));

        assertEquals(new BigDecimal("50.00"), cost);
    }

    @Test
    void roundsOnlyTheFinalTotalAndRoundsHalfUp() {
        when(zoneRules.findRule("DOMESTIC")).thenReturn(rule);
        when(rule.price(TEN_KG)).thenReturn(new BigDecimal("10.004"));
        when(heavyItemSurcharge.forWeight(TEN_KG)).thenReturn(new BigDecimal("0.001"));

        BigDecimal cost = calculator.calculateCost(shipment("DOMESTIC", ServiceLevel.STANDARD));

        // 10.004 + 0.001 = 10.005 -> 10.01. Rounding each part first would give 10.00,
        // and HALF_EVEN rounding would also give 10.00.
        assertEquals(new BigDecimal("10.01"), cost);
    }

    @Test
    void anUnknownZoneReachesTheCallerUnchanged() {
        UnknownZoneException failure = new UnknownZoneException("MOON");
        when(zoneRules.findRule("MOON")).thenThrow(failure);

        UnknownZoneException thrown = assertThrows(UnknownZoneException.class,
                () -> calculator.calculateCost(shipment("MOON", ServiceLevel.STANDARD)));

        assertSame(failure, thrown);
        verifyNoInteractions(heavyItemSurcharge);
    }

    @Test
    void aFailureInsideARuleIsNeverSwallowed() {
        // Regression guard for the original catch-all that turned any failure into a price of 0.
        IllegalStateException failure = new IllegalStateException("rule data corrupt");
        when(zoneRules.findRule("DOMESTIC")).thenReturn(rule);
        when(rule.price(any())).thenThrow(failure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> calculator.calculateCost(shipment("DOMESTIC", ServiceLevel.STANDARD)));

        assertSame(failure, thrown);
    }

    @Test
    void rejectsMissingDependenciesAndAMissingShipment() {
        assertThrows(NullPointerException.class, () -> new ShippingCalculator(null, heavyItemSurcharge));
        assertThrows(NullPointerException.class, () -> new ShippingCalculator(zoneRules, null));
        assertThrows(NullPointerException.class, () -> calculator.calculateCost(null));
    }
}