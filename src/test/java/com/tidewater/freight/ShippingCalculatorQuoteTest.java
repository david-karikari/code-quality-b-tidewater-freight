package com.tidewater.freight;

import static com.tidewater.freight.testsupport.Amounts.assertAmountEquals;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import com.tidewater.freight.app.ShippingCalculator;
import com.tidewater.freight.exception.UnknownZoneException;
import com.tidewater.freight.model.ServiceLevel;
import com.tidewater.freight.model.Shipment;
import com.tidewater.freight.model.ShippingQuote;
import com.tidewater.freight.pricing.HeavyItemSurcharge;
import com.tidewater.freight.repository.InMemoryZoneRuleRepository;
import org.junit.jupiter.api.Test;

class ShippingCalculatorQuoteTest {

    private final ShippingCalculator calculator = new ShippingCalculator(
            InMemoryZoneRuleRepository.withStandardRules(), new HeavyItemSurcharge());

    @Test
    void exposesEachStepOfTheCalculation() {
        ShippingQuote quote = calculator.quote(
                new Shipment("INTERNATIONAL", new BigDecimal("40"), ServiceLevel.EXPRESS));

        assertAmountEquals("625", quote.zonePrice());          // 15 x 40 + 25
        assertAmountEquals("20", quote.heavyItemSurcharge());  // over 30 kg
        assertEquals(ServiceLevel.EXPRESS, quote.serviceLevel());
        assertEquals(new BigDecimal("967.50"), quote.total()); // (625 + 20) x 1.5
    }

    @Test
    void showsNoSurchargeForALightShipment() {
        ShippingQuote quote = calculator.quote(
                new Shipment("DOMESTIC", new BigDecimal("10"), ServiceLevel.STANDARD));

        assertAmountEquals("50", quote.zonePrice());
        assertAmountEquals("0", quote.heavyItemSurcharge());
        assertEquals(new BigDecimal("50.00"), quote.total());
    }

    @Test
    void componentsAreExactAndOnlyTheTotalIsRounded() {
        ShippingQuote quote = calculator.quote(
                new Shipment("DOMESTIC", new BigDecimal("0.001"), ServiceLevel.STANDARD));

        assertAmountEquals("0.005", quote.zonePrice());        // 5 x 0.001, not rounded
        assertEquals(new BigDecimal("0.01"), quote.total());   // 0.005 rounds half up
    }

    @Test
    void calculateCostIsTheTotalOfTheQuote() {
        Shipment shipment = new Shipment("REGIONAL", new BigDecimal("31"), ServiceLevel.EXPRESS);

        assertEquals(new BigDecimal("417.00"), calculator.quote(shipment).total()); // (248 + 10 + 20) x 1.5
        assertEquals(calculator.quote(shipment).total(), calculator.calculateCost(shipment));
    }

    @Test
    void anUnknownZoneStillFailsWithTheZone() {
        Shipment shipment = new Shipment("MOON", BigDecimal.ONE, ServiceLevel.STANDARD);

        UnknownZoneException e = assertThrows(UnknownZoneException.class, () -> calculator.quote(shipment));

        assertEquals("MOON", e.getZone());
    }

    @Test
    void aMissingShipmentIsRejected() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> calculator.quote(null));

        assertTrue(e.getMessage().contains("shipment"));
    }
}