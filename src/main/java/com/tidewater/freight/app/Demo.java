package com.tidewater.freight.app;

import com.tidewater.freight.exception.ShippingException;
import com.tidewater.freight.model.ServiceLevel;
import com.tidewater.freight.model.Shipment;
import com.tidewater.freight.model.ShippingQuote;
import com.tidewater.freight.pricing.HeavyItemSurcharge;
import com.tidewater.freight.repository.InMemoryZoneRuleRepository;

import java.math.BigDecimal;

/** Console demonstration of the shipping calculator. */
public class Demo {

    public static void main(String[] args) {
        ShippingCalculator calculator = new ShippingCalculator(
                InMemoryZoneRuleRepository.withStandardRules(), new HeavyItemSurcharge());

        run(calculator, "DOMESTIC", "10", ServiceLevel.STANDARD);
        run(calculator, "INTERNATIONAL", "40", ServiceLevel.EXPRESS);
        run(calculator, "MOON", "10", ServiceLevel.STANDARD);
        run(calculator, "DOMESTIC", "-5", ServiceLevel.STANDARD);
        run(calculator, "DOMESTIC", "abc", ServiceLevel.STANDARD);
    }

    /** The single place where a shipping failure is caught: it is reported, never hidden. */
    private static void run(ShippingCalculator calculator, String zone, String weightKg, ServiceLevel level) {
        String label = zone + " " + weightKg + "kg " + level;
        try {
            Shipment shipment = new Shipment(zone, new BigDecimal(weightKg), level);
            ShippingQuote quote = calculator.quote(shipment);
            System.out.println(label + " -> GHS " + quote.total()
                    + " (zone price " + quote.zonePrice().toPlainString()
                    + " + heavy-item surcharge " + quote.heavyItemSurcharge().toPlainString()
                    + ", " + quote.serviceLevel() + ")");
        } catch (ShippingException e) {
            System.out.println(label + " -> rejected: " + e.getMessage());
        } catch (NumberFormatException e) {
        System.out.println(label + " -> rejected: weight is not a number: " + weightKg);
        }
    }
}