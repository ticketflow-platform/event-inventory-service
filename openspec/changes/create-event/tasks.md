## 1. Persistence Model

- [ ] 1.1 Add or update Flyway migrations for `events`, `categories`, `images`, `events_categories`, and `events_images` tables with UUID primary keys, foreign keys, and required constraints.
- [ ] 1.2 Align the `Event` entity with the contract: store the organizer JWT subject as a string, keep draft lifecycle timestamps, and remove the obsolete single `imageId` field.
- [ ] 1.3 Align category persistence with the administrator-managed active flag and keep the event-category link as a many-to-many association.
- [ ] 1.4 Align image persistence with reusable storage metadata and organizer ownership; use the existing `ImageType` on event-image links without adding a display-order field.
- [ ] 1.5 Add idempotency-key persistence for the organizer, request fingerprint, and created event reference.

## 2. Security And Configuration

- [ ] 2.1 Add the required Spring Security resource-server dependency and configure JWT issuer validation for Keycloak.
- [ ] 2.2 Map Keycloak `realm_access.roles` to Spring authorities and enable the `ORGANIZER` method-security boundary.
- [ ] 2.3 Configure the `/api/v1/events` route so missing or invalid tokens return `401` and users without `ORGANIZER` return `403`.

## 3. Event Creation API

- [ ] 3.1 Add request and response DTOs for `POST /api/v1/events` with `title`, `description`, `ageRestriction`, `categoryIds`, and typed `images` containing `imageId` and `ImageType`.
- [ ] 3.2 Implement the application service transaction that reads `sub`, validates references, creates a `DRAFT`, and persists category/image links atomically.
- [ ] 3.3 Implement `POST /api/v1/events` with `201 Created`, `Location`, ISO-8601 timestamps, and no client-supplied organizer or status fields.
- [ ] 3.4 Add structured validation and reference-error handling for blank titles, unsupported age restrictions, unknown or foreign images, and missing/inactive categories.
- [ ] 3.5 Ensure draft creation does not publish Kafka messages or create catalog integration events.
- [ ] 3.6 Require `Idempotency-Key`, return the original result for an identical retry, and reject key reuse with different content.

## 4. Verification

- [ ] 4.1 Add controller/integration tests for successful organizer draft creation and response fields.
- [ ] 4.2 Add authorization tests for unauthenticated users and authenticated users without `ORGANIZER`.
- [ ] 4.3 Add persistence tests proving category/image links are committed atomically and invalid references leave no event behind.
- [ ] 4.4 Add tests proving draft creation emits no Kafka/catalog event and stores timestamps as instants.
- [ ] 4.5 Add tests for idempotent retries, conflicting key reuse, default age restriction, and organizer-owned image enforcement.
- [ ] 4.6 Run the project test suite and verify the OpenAPI endpoint contract.
