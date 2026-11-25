## 1. Persistence Model

- [x] 1.1 Add or update Flyway migrations for `events`, `categories`, `images`, `events_categories`, and `events_images` tables with UUID primary keys, foreign keys, and required constraints.
- [x] 1.2 Align the `Event` entity with the contract: store the organizer JWT subject as a string, keep draft lifecycle timestamps, and remove the obsolete single `imageId` field.
- [x] 1.3 Align category persistence with the administrator-managed active flag and keep the event-category link as a many-to-many association.
- [x] 1.4 Align image persistence with reusable storage metadata and organizer ownership; use `ImagePosition` (`COVER`, `GALLERY`) on event-image links.
- [x] 1.5 Add idempotency-key persistence for the organizer, request fingerprint, and created event reference.

## 2. Security And Configuration

- [x] 2.1 Add the required Spring Security resource-server dependency and configure JWT issuer validation for Keycloak.
- [x] 2.2 Map Keycloak `realm_access.roles` to Spring authorities and enable the `ORGANIZER` method-security boundary.
- [x] 2.3 Configure the `/api/v1/events` route so missing or invalid tokens return `401` and users without `ORGANIZER` return `403`.

## 3. Event Creation API

- [x] 3.1 Add request and response DTOs for `POST /api/v1/events` with required `title`; age restriction, categories, images, and description are optional for drafts.
- [x] 3.2 Implement the application service transaction that reads `sub`, validates supplied references, creates a `DRAFT`, and explicitly persists category/image links atomically.
- [x] 3.3 Implement `POST /api/v1/events` with `201 Created`, ISO-8601 timestamps, and no client-supplied organizer or status fields.
- [x] 3.4 Add structured validation and reference-error handling for blank titles, unsupported supplied age restrictions, invalid cover counts, unknown or foreign images, and inactive categories.
- [x] 3.5 Ensure draft creation does not publish Kafka messages or create catalog integration events.
- [x] 3.6 Require `Idempotency-Key`, return the original result for an identical retry, and reject key reuse with different content.

## 4. Verification

- [x] 4.1 Add controller/integration tests for successful organizer draft creation and response fields.
- [x] 4.2 Add authorization tests for unauthenticated users and authenticated users without `ORGANIZER`.
- [x] 4.3 Add PostgreSQL integration tests proving supplied category/image links are persisted atomically and invalid references leave no event behind.
- [x] 4.4 Add tests proving draft creation emits no Kafka/catalog event and stores timestamps as instants.
- [x] 4.5 Add tests for idempotent retries, conflicting key reuse, optional draft fields, cover-image rules, and organizer-owned image enforcement.
- [x] 4.6 Run the project test suite and verify the OpenAPI endpoint contract.
