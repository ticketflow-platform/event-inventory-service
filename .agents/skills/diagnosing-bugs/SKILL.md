---
name: diagnosing-bugs
description: Diagnose reproducible bugs and regressions in this Java/Spring Boot service without guessing at product behavior or making unapproved design changes.
---

# Diagnose bugs in this Spring Boot service

Find the cause from evidence, fix the smallest coherent unit, and protect the real behavior with a clean regression test. Do not turn debugging into a mandatory ceremony: use only the steps that help answer this bug.

## 1. Establish the symptom and protect the workspace

- Read the root `AGENTS.md`, relevant OpenSpec requirements, nearby code/tests, and `git status`/diff. Preserve unrelated and uncommitted work.
- Separate **observed** from **expected** behavior. If either is missing and can't be derived from the contract, ask the user before choosing what the fix should do.
- Ask for a redacted exception, request, config, or reproduction only when it is needed. Never echo credentials, tokens, PII, or full auth headers.
- If the implementation and spec disagree, show both sides and wait for the owner's decision; don't silently decide which one is wrong.

## 2. Reproduce at the right seam

Prefer the smallest existing project test that can fail for this exact symptom:

1. HTTP/controller/security/validation bug: a parameterized `MockMvc` test using the existing `helper.BaseIntegrationTest` where applicable.
2. Service business-rule bug: a parameterized test of the public service method, mocking only boundaries needed for that scenario.
3. Transaction, query, or persistence bug: the real Testcontainers PostgreSQL setup and `@Sql` fixtures.
4. Use a running-service request or temporary harness only if existing test seams cannot reproduce the bug; explain why before adding tooling.

Use a focused Gradle test first (Windows: `.\gradlew.bat test --tests "fully.qualified.TestClass"`; Unix: `./gradlew test --tests "fully.qualified.TestClass"`). Confirm it fails for the reported reason, not a nearby failure. For intermittent issues, measure a useful reproduction rate; don't add arbitrary sleeps or loops without evidence.

If you cannot reproduce after reasonable focused attempts, stop. Tell the user what you tried and ask for the missing evidence or environment access. Do not guess a production fix.

## 3. Align on the fix before changing behavior

- For a clear, behavior-preserving root-cause fix with one obvious implementation, state the cause and intended minimal fix, then proceed.
- If the fix changes API behavior, status/error semantics, authorization, persistence/migration, compatibility, or architecture—or there are multiple sound fixes with different trade-offs—present the options and recommendation, ask the user, and wait. Never continue with “I'll assume X”.
- Prefer a targeted fix over a broad refactor. Do not add abstractions, dependencies, logging, temporary production instrumentation, scripts, or destructive data operations without a concrete need and approval where consequential.

## 4. Add a clean regression test

- Add a behavior test only when it would fail on the bug and pass after the fix. Use the project rules in `.agents/skills/tdd/SKILL.md`.
- Put all scenario inputs and expected outcomes in `<ProductionClass>TestCases` under `src/test/java/scenario/cases/java`; use parameterized JUnit 5 tests and combine scenarios that share setup, action, and assertion shape.
- Keep test bodies straight-line: parameter inputs, necessary stubs, one public call, complete AssertJ assertions. No test `if/else`, loops, reflection, private-method calls, inline scenario fixtures, or weakened assertions.
- Test the public API/public method. Never expose private implementation solely to test it.
- Aim to keep the 100% JaCoCo gate through meaningful cases. Do not add a behaviorless test to make a counter green. If a reported coverage gap has no meaningful public behavior behind it, explain the exact gap and ask before changing design, exclusions, or the gate.

## 5. Verify and report

- Run the regression test and verify it passes for the original symptom.
- Run broader tests and `gradlew check` when the change touches shared code, persistence, security, or coverage-sensitive logic.
- Remove only temporary instrumentation you introduced; never delete or revert user work.
- Report briefly: observed cause, behavior changed, tests run, and any remaining uncertainty. Don't claim a fix or coverage result that wasn't verified.
