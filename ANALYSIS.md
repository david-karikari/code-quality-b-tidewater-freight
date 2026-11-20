# Analysis: Inherited Tidewater Shipping Cost Service

Scope: `ShippingCalculator`, `RateRepository`, `InMemoryRateRepository`, `Demo`, and `ShippingCalculatorTest`, as received (before any change).

Evidence key: **[measured]** means I ran the unmodified code and observed the result. **[predicted]** means an expectation I reasoned out from reading the original tests. I did not run it.

## 1. Most serious problem

**`ShippingCalculator.calculateCost` swallows every failure and returns `0`** (`catch (Exception e) { return 0; }`, lines 34-37).

Why this outranks everything else:

- It turns a failed calculation into a valid-looking price of zero. A caller cannot tell "free shipment" from "the service broke".
- It hides other defects. A null service level, a null zone, a lower-case zone and an unknown zone all return `0.0` **[measured]**.
- The failure path logs nothing, so it is invisible even in the console output.
- The tests could not have caught it (section 3), so the suite gave false confidence.

The business impact is silent revenue loss, not an ugly design. The other problems below make the code hard to change; this one makes it wrong.

## 2. Code smells, anti-patterns and SOLID violations

| # | Problem | Where | Principle / smell | Evidence |
|---|---------|-------|-------------------|----------|
| 1 | Swallowed exception returns `0` | `ShippingCalculator` 34-37 | Anti-pattern: swallowed exception; error returned as valid data | `MOON` returns `0.0` in the demo **[measured]** |
| 2 | Dependency created inside the class (`new InMemoryRateRepository()`) | `ShippingCalculator` 6 | Violates Dependency Inversion; hard-wired collaborator; untestable seam | Tests cannot supply a different rate source; the comment on line 5 admits it |
| 3 | Zone fees in an `if / else if` chain on string literals | `ShippingCalculator` 14-20 | Violates Open/Closed; stringly-typed branching | A new zone needs edits to existing logic, which is the change the brief says is coming |
| 4 | New zone added to the repository but not to the chain is **priced silently with no fee** | `ShippingCalculator` 14-20 | Latent bug caused by #3: the `else` chain has no final `else` | Reading the code; no fee and no error for any zone not listed |
| 5 | One method does five jobs: fetch rate, compute base, add zone fee, add heavy surcharge, apply express multiplier, plus console output | `ShippingCalculator` 8-38 | Violates Single Responsibility; long method | Six distinct reasons to change in one method |
| 6 | Rules for one zone are split across two classes: base rate in the repository, fee in the calculator | `InMemoryRateRepository` 11-13, `ShippingCalculator` 14-20 | Scattered rule knowledge; shotgun surgery when a zone changes | Adding a zone touches both files |
| 7 | `System.out.println` inside business logic, on the success path only | `ShippingCalculator` 32 | Side effect in a calculation; no logging abstraction | Output cannot be asserted or silenced |
| 8 | Money as `double` | whole class, `RateRepository` | Floating-point error in currency | `REGIONAL`, 0.1 kg, `EXPRESS` gives `16.200000000000003` **[measured]** |
| 9 | Magic numbers: `10`, `25`, `30`, `20`, `1.5` | `ShippingCalculator` | Unnamed constants; the rules are not documented anywhere | No names for threshold, fees, multiplier |
| 10 | Zone and service level are raw `String`s | method signature | Primitive obsession; invalid values are representable | `"EXPRES"` (typo) is silently priced as STANDARD, giving `50.0` **[measured]** |
| 11 | No input validation | method signature | Missing guard clauses | Weight `-10` returns `-50.0`; `NaN` returns `NaN` **[measured]** |
| 12 | Failure contract is unclear and generic | `InMemoryRateRepository` 20 | A bare `IllegalArgumentException` carries no typed meaning | The only failure the repository raises is then caught and erased by the calculator |
| 13 | Order of operations is an undocumented business rule | `ShippingCalculator` 11-30 | Hidden rule | Express (x1.5) applies after fees and surcharge; `INTERNATIONAL`, 40 kg, `EXPRESS` = ((15x40)+25+20)x1.5 = 967.5 |
| 14 | `Demo` presents the swallowed failure as a normal result | `Demo` 9 | Misleading output | Prints `0.0` for the unknown zone **[measured]** |

Open question for the business, not a bug: the heavy surcharge uses `weightKg > 30`, so exactly 30 kg is not heavy. `DOMESTIC` at 30 kg costs `150.0` and at 30.01 kg costs `170.05` **[measured]**. I will keep the behaviour, name the rule, and pin the boundary with tests, but the threshold should be confirmed with the lead.

## 3. AI-generated tests that do not test anything

| Test | What it actually checks | Why it proves nothing | Would still pass if... |
|------|-------------------------|-----------------------|------------------------|
| `calculatesACost` | `cost > 0` for `DOMESTIC`, 10 kg, `STANDARD` | Accepts any positive number. The correct value is `50.0`, and it never states it. | the `DOMESTIC` rate were changed from 5.0 to 6.0, or the cost were hard-coded to `1` **[predicted]** |
| `calculatesExpressCost` | A **mock** of `ShippingCalculator` returns the value it was told to return | The class under test is replaced by a mock, so no production code runs. It tests Mockito, not the calculator. | `calculateCost` were deleted or its body returned `0` **[predicted]** |
| `returnsACostForAShipment` | `assertNotNull(cost)` on a primitive `double` | A `double` autoboxes to a non-null `Double`, so the assertion **cannot fail**. | the method returned `0`, `-1` or `NaN` **[predicted]** |

Why they got through: all three read as reasonable test names, all are green, and none states an expected value. `calculatesExpressCost` even contains the correct answer (`967.5`), but only as something the mock is told to say.

Missing coverage:

- Exact prices for each zone, with and without express.
- The heavy-item boundary (30 vs 30.01 kg) and the fees at that boundary.
- Unknown zone, null zone, lower-case zone.
- Null, unknown or misspelled service level.
- Zero, negative, `NaN` and infinite weight.
- Rounding behaviour.
- Anything that would fail if the repository were replaced, because it could not be replaced.

## 4. What this means for the refactor

| Problem group | Planned fix | Deliverable |
|---------------|-------------|-------------|
| #2, #5, #6, #7 | Inject the rate source; split fetching, pricing rules and the calculator; remove console output from the calculation | Refactor |
| #3, #4, #9, #13 | One pricing-rule object per zone behind an interface, so a new zone is a new class and not an edit; named constants; documented order of operations | Refactor, rationale |
| #1, #10, #11, #12, #14 | Typed zone and service level; guard clauses; custom exceptions carrying zone, weight and service level; nothing swallowed | Exception handling |
| #8 | `BigDecimal` money with one explicit rounding rule at the boundary | Refactor |
| Section 3 | Replace all three tests with exact-value, boundary, failure-case JUnit 5 and Mockito tests; confirm with JaCoCo and PIT | Test suite |