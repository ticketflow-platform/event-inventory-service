# Mocking in this Spring Boot service

Use a mock only when it makes a focused behavior test simpler without hiding the behavior under test.

## Good boundaries

- In a public service-method unit test, Mockito can stand in for repository ports or genuinely external collaborators so the test can isolate a business rule.
- For database queries, transaction boundaries, Flyway mappings, and persisted state, use the real PostgreSQL Testcontainers setup. A repository mock cannot prove those behaviors.
- For HTTP, authorization, and validation behavior, use the existing Spring test setup and `MockMvc`; don't mock the filter chain or controller behavior away.
- Use real DTOs, entities, enums, value objects, and deterministic mappers. Prefer a real mapper implementation over a mock when it is available and keeps the test focused.

Follow the nearby test convention where it is sound. If following it would make the test depend on incidental interactions or require a broad test redesign, identify that trade-off and ask before expanding scope.

## Mockito rules

- Keep stubbing explicit and driven by test parameters. No `thenAnswer` logic with branches to manufacture several scenarios inside one test.
- Avoid `verify` of incidental call counts and ordering. Assert the returned response, domain error, HTTP contract, or persisted state. Verify interactions only when the interaction itself is a required externally meaningful effect, such as avoiding a duplicate external publication.
- Don't mock request/response DTOs, JPA entities, enums, collections, or simple values.
- Don't introduce an interface or production seam solely to make a mock possible. Ask before adding an abstraction whose only motivation is test convenience.
- Never test private methods by reflection or change production visibility for tests.

## Test data and readability

All scenario inputs and expected results come through the parameterized method's `@MethodSource`. The test body contains only the needed stubs, one call to the public seam, and complete AssertJ assertions. Keep stubs minimal; remove any stub that does not affect the scenario.
