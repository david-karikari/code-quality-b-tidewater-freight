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
| Run the demo | `mvn compile`, then `java -cp target/classes com.tidewater.freight.app.Demo` |

Expected demo output:

```
DOMESTIC 10kg STANDARD -> GHS 50.00 (zone price 50 + heavy-item surcharge 0, STANDARD)
INTERNATIONAL 40kg EXPRESS -> GHS 967.50 (zone price 625 + heavy-item surcharge 20, EXPRESS)
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

```
com.tidewater.freight
├── app
│   ├── ShippingCalculator
│   └── Demo
├── model
│   ├── Shipment
│   ├── ShippingQuote
│   └── ServiceLevel
├── pricing
│   ├── ZonePricingRule
│   ├── FlatRateZoneRule
│   └── HeavyItemSurcharge
├── repository
│   ├── ZoneRuleRepository
│   └── InMemoryZoneRuleRepository
└── exception
    ├── ShippingException
    ├── UnknownZoneException
    ├── InvalidWeightException
    └── InvalidShipmentException
```

Each class has one job. Dependencies are passed in through constructors and point one way: `exception` and `pricing` depend on nothing else in the project, `model` depends on `exception`, `repository` depends on `pricing` and `exception`, and `ShippingCalculator` depends on all four. Tests mirror this layout, and the shared test helper `Amounts` is in `testsupport`.

| Class | Package | Responsibility |
|---|---|---|
| `ShippingCalculator` | app | Combines the zone price, the surcharge and the service level, then rounds. Contains no zone names and no numbers. |
| `Demo` | app | Console entry point. It is the single place that catches `ShippingException` and reports it. |
| `Shipment` | `model` | The request: zone, weight and service level. It rejects invalid values when it is created, so the calculator never sees them. |
| `ShippingQuote` | `model` | The price with each step that produced it. |
| `ServiceLevel` | `model` | Enum of the delivery speeds and their multipliers. |
| `ZonePricingRule` | `pricing` | Interface for one zone's pricing. |
| `FlatRateZoneRule` | `pricing` | A zone priced as a rate per kg plus a fixed fee. |
| `HeavyItemSurcharge` | `pricing` | The over-30 kg rule, with the threshold and amount as named constants. |
| `ZoneRuleRepository` | `repository` | Interface for finding the pricing rule of a zone. |
| `InMemoryZoneRuleRepository` | `repository` | Holds the rules in memory. `withStandardRules()` lists Tidewater's current zones. |
| `ShippingException` | `exception` | Base type for every failure the service reports on purpose. |
| `UnknownZoneException` | `exception` | No pricing rule exists for the zone. Carries the zone. |
| `InvalidWeightException` | `exception` | The weight is missing, zero or negative. Carries the zone and the weight. |
| `InvalidShipmentException` | `exception` | The zone or service level is missing. |

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
6. There is no maximum shipment weight, because the brief gives none

## What was kept, fixed or rewritten

## Refactoring decisions

| Item | Verdict | Reason |
|---|---|---|
| Rates, fees, the 30 kg threshold, the 20 surcharge, the 1.5 multiplier | Kept | They are the business rules, and they were correct. They now sit in named, tested places. |
| Order of operations | Kept | It was the original business rule. It is now documented and guarded by a test that fails if it changes. |
| Repository abstraction | Kept the idea, rewritten | The seam was good, but it returned a bare rate and was created inside the calculator. It now returns a rule and is injected. |
| `ShippingCalculator` | Rewritten | It did five jobs, swallowed every failure and printed to the console. |
| `catch (Exception)` returning 0 | Rewritten | Failures now reach the caller as `ShippingException`. |
| `double` arithmetic | Rewritten | Replaced with `BigDecimal` and one rounding step. |
| Chain of zone conditions | Rewritten | One `ZonePricingRule` per zone. |
| `Demo` | Kept, adapted | The same scenarios, plus invalid input. It reports failures and shows the price breakdown. |
| AI test `calculatesACost` | Rewritten | It asserted only `cost > 0`. |
| AI test `calculatesExpressCost` | Rewritten | It tested a mock of the class under test, so no production code ran. |
| AI test `returnsACostForAShipment` | Rewritten | `assertNotNull` on a primitive `double` cannot fail. |
| `pom.xml`, JaCoCo and PIT setup | Kept, extended | Mockito runs as a Java agent, and `Demo` is excluded from the reports. |


## Testing

The suite has 72 tests, in JUnit 5 with Mockito. It runs with `mvn test`.

| Test class | Tests | What it covers |
|---|---|---|
| `ShippingCalculatorPricingTest` | 15 | Exact prices with the real collaborators, including the 30 kg boundary, rounding and a new zone added without touching the calculator |
| `ShippingCalculatorTest` | 6 | The calculator's own job with mocked collaborators: order of operations, rounding, and failures reaching the caller |
| `ShipmentTest` | 13 | Validation of zone, weight and service level, and what each exception carries |
| `InMemoryZoneRuleRepositoryTest` | 11 | Lookup, the standard rates and fees, unknown zones, duplicate rules |
| `HeavyItemSurchargeTest` | 7 | The threshold at 29.99, 30, 30.00 and 30.01 kg |
| `FlatRateZoneRuleTest` | 4 | Rate per kg plus fixed fee |
| `ServiceLevelTest` | 4 | The multipliers, and that `apply` does not round |
| `ShippingCalculatorQuoteTest` | 6 | The itemised quote: each component is exact, only the total is rounded, and `calculateCost` equals the quote total |

### What makes these tests trustworthy

- **Every test asserts a real value or a thrown exception.** None of them can pass on `> 0` or a non-null check, which is how the AI-generated tests passed.
- **Money is compared by value in the unit tests.** `Amounts.assertAmountEquals` uses `compareTo`, with expected values written as strings. Calculator totals are compared with `assertEquals` on purpose, so the two-decimal scale is checked as well.
- **Mocks replace collaborators, never the class under test.** The Mockito tests assert the price that comes back. A `verify` is only a secondary check.
- **Exceptions are asserted by type and by the data they carry** (`getZone()`, `getWeightKg()`), plus that the message names the offending values. Whole messages are not compared, so rewording one does not break a test.
- **No test compares console output.** `Demo` is excluded from the reports for that reason (see Known limitations).
- **Test values were chosen to catch specific mistakes.** For example, a rate of 8 and a weight of 3 give 34, which differs from 8 + 3 + 10 and from 8 x 3 - 10. Rounding 10.005 gives 10.01 half up but 10.00 half even.

### Cases covered

| Kind | Examples |
|---|---|
| Happy path | Each zone at standard and express; the combined order of surcharge and multiplier |
| Edge cases | 29.99, 30, 30.00 and 30.01 kg; 0.1 kg; the smallest accepted weight of 0.001 kg; half-up rounding at 58.275, 224.925, 2317.275 and 10.005; results always at two decimal places |
| Failures | Unknown, lower-case, null and blank zone; zero, negative and missing weight; missing service level; null shipment; null dependencies; duplicate zone rules; a failure inside a rule reaching the caller unchanged |

`aFailureInsideARuleIsNeverSwallowed` and `anUnknownZoneReachesTheCallerUnchanged` are the tests that would have failed against the original `catch (Exception) { return 0; }`.

### Results

Run under JDK 21.

| Measure | Result |
|---|---|
| Tests | 72 run, 0 failures |
| JaCoCo line coverage | 100% (69 / 69 lines) |
| JaCoCo instruction coverage | 100% (354 / 354) |
| JaCoCo branch coverage | 100% (20 / 20) |
| PIT mutation coverage | 100% (18 / 18) |
| PIT test strength | 100% (18 / 18) |

## Coverage by package

| Package | Line coverage | Branch coverage | Mutation coverage | Test strength |
|---|---|---|---|---|
| `com.tidewater.freight.model` | 100% (16 / 16) | 100% (10 / 10) | 100% (1 / 1) | 100% (1 / 1) |
| `com.tidewater.freight.repository` | 100% (12 / 12) | 100% (6 / 6) | 100% (4 / 4) | 100% (4 / 4) |
| `com.tidewater.freight.pricing` | 100% (15 / 15) | 100% (4 / 4) | 100% (8 / 8) | 100% (8 / 8) |
| `com.tidewater.freight.app` | 100% (12 / 12) | n/a | 100% (2 / 2) | 100% (2 / 2) |
| `com.tidewater.freight.exception` | 100% (14 / 14) | n/a | 100% (3 / 3) | 100% (3 / 3) |
| **Total** | **100% (69 / 69)** | **100% (20 / 20)** | **100% (18 / 18)** | **100% (18 / 18)** |

Line and branch figures come from JaCoCo. Mutation coverage and test strength come from PIT. The `app` and `exception` packages contain no branches, so branch coverage does not apply.


`Demo` is excluded from both reports. Historical, for context only: an early run, before `Demo` was excluded and before `FlatRateZoneRule` validated its inputs, showed 82% instruction coverage in JaCoCo with the only uncovered code in `Demo`, and 14 of 20 mutants killed in PIT with none surviving. The current figures are the ones in the table above.

### Manual mutation checks

PIT's default mutators change primitive operators and comparisons. This code does its arithmetic with `BigDecimal` method calls, which PIT does not change, so a clean PIT result alone proves less here than usual. To cover that, five deliberate bugs were introduced one at a time and the suite was run after each. Every change was reverted afterwards.

| # | Deliberate bug | Failures | Failing tests |
|---|---|---|---|
| 1 | `HeavyItemSurcharge`: `> 0` changed to `>= 0` | 3 | HeavyItemSurchargeTest.chargesOnlyAboveThirtyKilograms (cases 3 and 4); ShippingCalculatorPricingTest.pricesEachShipmentExactly (case 7) |
| 2 | `ServiceLevel`: express `"1.5"` changed to `"1.4"` | 14 | ServiceLevelTest (applyDoesNotRound, expressMultipliesTheCostByOnePointFive, expressOnALargeCostIsExact); ShippingCalculatorPricingTest (aNewZoneWithItsOwnRulesIsPricedWithoutChangingTheCalculator; pricesEachShipmentExactly cases 2, 4, 6, 10, 11, 12, 13); ShippingCalculatorQuoteTest (exposesEachStepOfTheCalculation, calculateCostIsTheTotalOfTheQuote); ShippingCalculatorTest (addsTheSurchargeBeforeApplyingTheServiceLevel) |
| 3 | `ShippingCalculator`: `HALF_UP` changed to `HALF_EVEN` | 3 | ShippingCalculatorPricingTest.pricesEachShipmentExactly (case 11); ShippingCalculatorQuoteTest.componentsAreExactAndOnlyTheTotalIsRounded; ShippingCalculatorTest.roundsOnlyTheFinalTotalAndRoundsHalfUp |
| 4 | `FlatRateZoneRule`: `.add(fixedFee)` removed | 13 | FlatRateZoneRuleTest (pricesRatePerKgTimesWeightPlusFixedFee, fractionalWeightsAreExact); InMemoryZoneRuleRepositoryTest.standardRulesCarryTheCurrentRatesAndFees (cases 2 and 3); ShippingCalculatorPricingTest.pricesEachShipmentExactly (cases 3, 4, 5, 6, 9, 12, 13); ShippingCalculatorQuoteTest (exposesEachStepOfTheCalculation, calculateCostIsTheTotalOfTheQuote) |
| 5 | `ShippingCalculator`: service level applied before the surcharge | 5 | ShippingCalculatorPricingTest.pricesEachShipmentExactly (cases 6 and 13); ShippingCalculatorQuoteTest (exposesEachStepOfTheCalculation, calculateCostIsTheTotalOfTheQuote); ShippingCalculatorTest.addsTheSurchargeBeforeApplyingTheServiceLevel |

## Beyond the brief: itemised price breakdown

`ShippingCalculator.quote(shipment)` returns a `ShippingQuote` with each step of the calculation: the zone price, the heavy-item surcharge, the service level and the rounded total. `calculateCost` is unchanged and returns the total of the same quote.

Why it is worth having:

- **A charge can be explained.** Support staff can tell a customer why a shipment cost 967.50, and finance can reconcile it, without repeating the arithmetic by hand.
- **The order of operations is visible.** In the original code it was an undocumented business rule hidden inside one method.
- **Failures are easier to locate.** The tests assert each step on its own, so a wrong rule shows up as a wrong component, not as a wrong total with no explanation.

The components are exact and not rounded, so they can have more than two decimal places. Only the total is rounded. This matters: rounding each line and then adding them can disagree with the rounded total by one pesewa.

## What to look out for in AI-generated code

These are the mistakes this exercise found. Check for them in review:

1. **A catch-all that returns a normal-looking value.** A failed calculation became a price of zero.
2. **Tests that assert almost nothing.** `> 0` and "is not null" on a number accept almost any answer.
3. **A test that mocks the class it is testing.** It proves the mock works, not the code.
4. **Hidden dependencies.** A `new` inside the class made it impossible to test without the real collaborator.
5. **Business rules hard-coded as a chain of conditions.** Adding a zone meant editing working code, and a zone missed in the chain was priced with no fee and no error.
6. **`double` for money.** It was correct on the happy path and wrong on the cents.
7. **Green tests taken as proof.** The original suite passed. Only checking what each test could fail on showed it proved nothing.

## Known limitations

| Limitation | Notes |
|---|---|
| Rates and fees are defined in code | A change needs a redeploy. `ZoneRuleRepository` is an interface, so a database or file implementation can replace the in-memory one without touching the calculator. |
| Zone codes are free strings | A mistyped zone fails at runtime with `UnknownZoneException`, which names the zone. It is not caught at compile time. |
| One currency and one unit | Prices are in GHS and weights in kg. Neither is a type. |
| `Demo` is not covered by tests | It is excluded from the coverage and mutation reports because it is a console entry point. Its one catch block is checked by running it. |
| PIT cannot mutate `BigDecimal` arithmetic | The manual mutation checks above cover this. They were done by hand and are not repeated automatically. |
| No logging | Failures are thrown to the caller and reported at the edge, in `Demo.run`, with `System.out`. A logger would be added in the caller, next to the single catch. |