# Tidewater Freight: Shipping Cost Service

Calculates the cost of a shipment, in GHS, from its destination zone, its weight and its service level.

This repository is the refactored version of an AI-generated service that compiled and passed its tests but hid failures and could not be tested properly. The analysis of what was wrong is in [ANALYSIS.md](ANALYSIS.md).

## Running it

Requires JDK 21 and Maven.

| Task | Command |
|---|---|
| Run the tests | `mvn test` |
| Tests plus coverage report | `mvn verify`, then open `target/site/jacoco/index.html` |
| Mutation testing | `mvn test-compile org.pitest:pitest-maven:mutationCoverage`, then open the newest folder under `target/pit-reports/` |
| Run the demo | `mvn compile`, then `java -cp target/classes com.tidewater.freight.Demo` |

Expected demo output:

```
DOMESTIC 10kg STANDARD -> GHS 50.00
INTERNATIONAL 40kg EXPRESS -> GHS 967.50
MOON 10kg STANDARD -> rejected: No pricing rule for zone: MOON
DOMESTIC -5kg STANDARD -> rejected: Weight must be greater than zero for zone DOMESTIC but was -5
```

## Pricing rules

For a shipment, the cost is worked out in this order:

1. The zone's price: rate per kg multiplied by the weight, plus the zone's fixed fee.
2. Plus a flat 20 if the weight is over 30 kg. Exactly 30 kg is not heavy.
3. Multiplied by the service level: 1.0 for `STANDARD`, 1.5 for `EXPRESS`.
4. Rounded once, to two decimal places, half up.

| Zone | Rate per kg | Fixed fee |
|---|---|---|
| `DOMESTIC` | 5 | none |
| `REGIONAL` | 8 | 10 |
| `INTERNATIONAL` | 15 | 25 |

Example: `INTERNATIONAL`, 40 kg, `EXPRESS` is (15 x 40 + 25 + 20) x 1.5 = 967.50.

## Structure

Each class has one job. Dependencies are passed in through constructors.

| Class | Responsibility |
|---|---|
| `ShippingCalculator` | Combines the zone price, the surcharge and the service level, then rounds. Contains no zone names and no numbers. |
| `Shipment` | The request: zone, weight and service level. It rejects invalid values when it is created, so the calculator never sees them. |
| `ZoneRuleRepository` | Interface for finding the pricing rule of a zone. |
| `InMemoryZoneRuleRepository` | Holds the rules in memory. `withStandardRules()` lists Tidewater's current zones. |
| `ZonePricingRule` | Interface for one zone's pricing. |
| `FlatRateZoneRule` | A zone priced as a rate per kg plus a fixed fee. |
| `HeavyItemSurcharge` | The over-30 kg rule, with the threshold and amount as named constants. |
| `ServiceLevel` | Enum of the delivery speeds and their multipliers. |
| `ShippingException` | Base type for every failure the service reports on purpose. |
| `UnknownZoneException` | No pricing rule exists for the zone. Carries the zone. |
| `InvalidWeightException` | The weight is missing, zero or negative. Carries the zone and the weight. |
| `InvalidShipmentException` | The zone or service level is missing. |
| `Demo` | Console entry point. It is the single place that catches `ShippingException` and reports it. |

## Adding a new zone

A new zone does not change the calculator or any existing rule.

**If the zone is priced like the others** (a rate per kg plus a fixed fee), add one line to `InMemoryZoneRuleRepository.withStandardRules()`:

```java
// Illustrative example only, not a real Tidewater rule.
new FlatRateZoneRule("ARCTIC", new BigDecimal("20"), new BigDecimal("40"))
```

**If the zone has its own logic** (for example, tiered rates), write a class that implements `ZonePricingRule` and register it in the same list. `ShippingCalculatorPricingTest` contains a working example, `TieredArcticRule`, and tests that it is priced correctly while the existing zones stay unchanged.

## Design decisions

| Decision | Why | Trade-off |
|---|---|---|
| One pricing rule per zone behind the `ZonePricingRule` interface | The business said a new zone is coming. A new zone is a new rule, so no existing code is edited (Open/Closed). | One more interface than a switch statement would need. |
| A zone is a `String` code, not an enum | An enum would have to be edited for every new zone, which is the change we are avoiding, and it could not hold different logic per zone. | A mistyped zone is a runtime `UnknownZoneException`, not a compile error. The exception carries the zone, and a test covers it. |
| Validation lives in the `Shipment` constructor | An invalid shipment cannot exist, so the calculator never needs to check its input. | Callers build a `Shipment` before asking for a price. |
| One exception family, `ShippingException`, unchecked | Failures are specific and carry context (zone, weight). Nothing is caught inside the domain code, and there is one catch at the edge (`Demo`). | Callers are not forced by the compiler to handle failures. |
| `BigDecimal` for money and weight, built from strings | `double` gave wrong cents. Weight as `BigDecimal` also removes `NaN` and infinity as inputs. | Slightly more verbose arithmetic. |
| Rounding happens once, on the final total | Rounding intermediate values compounds errors. | None. The order of operations is documented in the class. |
| `HeavyItemSurcharge` is its own class, injected | One reason to change per class, and the calculator can be tested with it mocked. | One more constructor argument. |
| Constructor injection with null checks | Dependencies are explicit and cannot be missing. This is what makes the Mockito tests possible. | None. |
| Duplicate zone rules throw `IllegalArgumentException` | It is a configuration mistake by the programmer, not a failure a customer causes. | It sits outside `ShippingException` on purpose. |

## Behaviour changes from the original

The prices of valid shipments are unchanged. These cases behave differently, on purpose:

| Case | Original | Now |
|---|---|---|
| Unknown zone | Returned `0.0` | `UnknownZoneException` naming the zone |
| Negative weight | Returned a negative price (`-50.0` for 10 kg) | `InvalidWeightException` |
| Zero weight | Priced as the zone fee only | `InvalidWeightException` |
| Missing zone or service level | Returned `0.0` | `InvalidShipmentException` |
| Zone in lower case, such as `domestic` | Returned `0.0` | `UnknownZoneException` (zone codes are case-sensitive) |
| Misspelled service level, such as `EXPRES` | Silently priced as standard | Cannot be written: `ServiceLevel` is an enum |
| Rounding | None, so results like `16.200000000000003` | Two decimal places, half up, such as `16.20` |
| Output format | `50.0` | `50.00` |

Compared against the original code on 78 combinations of zone, weight and service level, 75 gave the same price. The other 3 differ by one pesewa because `double` rounded wrongly. For example, `DOMESTIC`, 7.77 kg, `EXPRESS` is exactly 58.275, which the old code turned into 58.27499… and rounded down. The new code gives 58.28.

## Assumptions to confirm with the engineering lead

These were not in the brief. Each is a single place to change if the answer differs.

1. A weight of zero is invalid.
2. A shipment of exactly 30 kg is not heavy. The original used `> 30`, and this was kept.
3. Zone codes are case-sensitive.
4. Rounding to two decimal places, half up, is the right rule for GHS.
5. The express multiplier applies after the zone fee and the heavy-item surcharge. This was the original order and was kept.

## What was kept, fixed or rewritten

| Item | Verdict | Reason |
|---|---|---|
| Rates, fees, the 30 kg threshold, the 20 surcharge, the 1.5 multiplier | Kept | They are the business rules