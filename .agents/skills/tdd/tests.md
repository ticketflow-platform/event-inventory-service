# Tests that fit this repository

Tests describe behavior callers care about and survive refactors. Use the smallest public seam that proves the requirement.

## Test shape

Use JUnit 5 `@ParameterizedTest` with scenario inputs and expected values supplied by a `<ProductionClass>TestCases` method in `src/test/java/scenario/cases/java`.

```java
@ParameterizedTest(name = "{index}: значение {0}")
@MethodSource("scenario.cases.java.domain.model.entity.enums.AgeRestrictionTestCases#isSupport_whenValueIsChecked_thenReturnSupportStatusTestCases")
@DisplayName("Проверяет поддержку возрастного ограничения по сценариям")
void isSupport_whenValueIsChecked_thenReturnSupportStatusTest(
    Integer value,
    boolean expected) {
    Assertions.assertThat(AgeRestriction.isSupport(value)).isEqualTo(expected);
}
```

The test body receives all scenario inputs from parameters and stays straight-line: necessary stubs, one public call, complete AssertJ assertions. Do not put `if/else`, loops, `switch`, reflection, helper methods, or scenario-specific fixture construction in the test. If assertion logic differs, create another parameterized method instead of branching.

## Choose the seam

- HTTP, validation, security, serialization, and error contracts: use `MockMvc` and the existing `helper.BaseIntegrationTest` where applicable. Assert the status and all stable, contract-relevant response fields. For dynamic values such as generated IDs or timestamps, assert their defined format/relationship rather than hard-coding a value the test cannot control.
- Service business rules: call the public service method. Mockito may stub repositories or external boundaries; assert the complete return value or domain exception.
- Transactions, queries, migrations, and persisted state: use the real PostgreSQL Testcontainers setup and `@Sql` fixtures. Database state is a valid assertion when persistence itself is part of the behavior.
- MapStruct: test its public mapping contract. Never call private helpers or widen production visibility to test them.

## Scenario grouping

Keep `Arguments.of(...)`, request bodies, expected values, entities, and reusable fixtures in `<ProductionClass>TestCases`. The test method gets every scenario input from `@MethodSource`; do not create special request/response/entity fixtures inside it.

Combine cases in one parameterized method when setup, action, and assertion shape are the same. Split cases only when they genuinely need different setup or assertions. A single meaningful case is acceptable under the repository convention; do not manufacture extra cases to fill out a provider.

## Avoid false confidence

- Expected values must come from the contract, a worked example, or an independent literal—not the same calculation as production.
- Assert the full meaningful contract. Don't use `contains(...)` when stable complete values can be compared; don't compare unstable generated values to invented constants.
- Don't test a class or method just because it exists or is uncovered. The test must protect behavior a caller or stated invariant relies on.
- Don't mock DTOs, entities, enums, collections, or simple value objects. Don't mock persistence in a test whose purpose is transaction/query behavior.
- Don't assert incidental mock call counts/order. Verify an interaction only when it represents a required externally meaningful effect.
- The 100% JaCoCo gate is real, but coverage must come from meaningful cases. If a counter can only be satisfied by a contrived test, surface that exact conflict and ask before changing code, exclusions, or the gate.
