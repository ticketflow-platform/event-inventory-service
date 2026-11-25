---
name: tdd
description: Test-driven development for this Java/Spring Boot service. Use for behavior changes, fixes, and meaningful API or persistence tests.
---

# TDD for this Spring Boot service

Use red → green → refactor to protect real behavior. The goal is clean production code and useful tests, not a large test diff or coverage numbers in isolation. Read the root `AGENTS.md`, `openspec/config.yaml`, the relevant OpenSpec requirement, and adjacent code/tests before choosing a seam.

## Align before implementation

1. State the requested behavior and what the existing code/spec actually says; separate facts from assumptions.
2. Identify the public seam and the smallest useful test group.
3. If behavior, API/error semantics, persistence, security, compatibility, architecture, or scope has more than one reasonable interpretation, ask a concise question with options, consequences, and a recommendation. **Wait for the answer; do not implement your preferred assumption.**
4. If the request and repository conventions make the behavior and solution clear, don't ask ritual questions. Briefly state the plan for a non-trivial change and proceed.

If the implementation and spec disagree, surface the discrepancy before changing behavior. Do not silently “fix” either side.

## Choose a meaningful Spring test seam

- Use `MockMvc` and the existing `helper.BaseIntegrationTest` for HTTP behavior: status, validation, serialization, security, localized errors, and API-visible results.
- Use a public service method with Mockito at repository/external boundaries when the business rule is the subject and a full Spring context adds no value.
- Use the real Testcontainers PostgreSQL setup and `@Sql` scripts for transaction, schema, query, and persisted-state guarantees.
- Test a MapStruct mapper through its public mapping contract. Never call private methods, use reflection, or widen production visibility to make code testable.
- Prefer real DTOs, entities, enums, and deterministic mapping code over mocks. See [mocking.md](mocking.md).

Choose a seam because it proves the requested behavior, not because it is the easiest way to reach a line or branch.

## Write clean, parameterized tests

- Use JUnit 5 `@ParameterizedTest` and `@MethodSource`; keep scenario arguments and scenario-specific fixtures in `<ProductionClass>TestCases` under `src/test/java/scenario/cases/java`.
- Combine cases in one parameterized test when setup, action, and assertion shape are the same. Split only when those genuinely differ. A one-case provider is acceptable when required by project convention; don't invent extra cases to make a test look parameterized.
- Pass every scenario input and expected outcome as test parameters. The test body may arrange mocks from those parameters, invoke the public behavior, and assert. It must not contain `if/else`, loops, `switch`, reflection, private test helpers, or scenario-specific fixture construction.
- Use AssertJ only. Assert the complete meaningful response/error or persisted outcome; avoid weak substring assertions when an exact assertion is practical.
- Keep one clear behavior per test method. If cases have different assertion logic, use separate parameterized methods instead of branching inside a test.
- Use Russian `@DisplayName` and the project's existing naming conventions.

Examples and mocking boundaries: [tests.md](tests.md), [mocking.md](mocking.md).

## Red → green → refactor

1. Add the smallest parameterized behavior test that fails for the reported/missing behavior, using independent expected values from the spec or user-approved contract.
2. Run that test and confirm it fails for the intended reason.
3. Make the smallest cohesive production change that satisfies the behavior; don't add speculative abstractions or unrelated cleanup.
4. Run the focused test, then the relevant broader suite and `gradlew check` as appropriate. Review the diff and preserve unrelated work.

The project requires at least 95% line and branch coverage of handwritten production code. Generated MapStruct implementations stay in the report, but do not count towards the gate. Analyze uncovered code against requirements even when the gate passes, and add missing meaningful behavior scenarios. Do not add dummy controllers, probe endpoints, or tests that only game a counter. If a gap has no meaningful scenario through the real public contract, explain it and ask before changing the design, exclusions, or gate.
