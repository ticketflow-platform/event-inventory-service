## ADDED Requirements

### Requirement: Organizer can create an event draft
The inventory service SHALL expose `POST /api/v1/events` and SHALL create an event with status `DRAFT` for an authenticated user with the `ORGANIZER` role. The request SHALL include an `Idempotency-Key` header.

#### Scenario: Draft is created successfully
- **WHEN** an authenticated organizer submits a valid event title and optional description, age restriction, category IDs, and typed image references
- **THEN** the service SHALL persist the event and return `201 Created` with its generated identifier and `DRAFT` status
- **AND** the response SHALL include the organizer identifier from the JWT `sub` claim

#### Scenario: Omitted age restriction uses the default
- **WHEN** an authenticated organizer omits `ageRestriction`
- **THEN** the service SHALL persist age restriction `0`
- **AND** `0` SHALL mean that no minimum age restriction is configured

#### Scenario: Non-organizer cannot create a draft
- **WHEN** an authenticated user without the `ORGANIZER` role submits the request
- **THEN** the service SHALL return `403 Forbidden`

#### Scenario: Request is unauthenticated
- **WHEN** the request has no valid bearer token
- **THEN** the service SHALL return `401 Unauthorized`

### Requirement: Event ownership comes from verified identity
The service SHALL ignore any organizer identifier supplied by a client and SHALL persist the authenticated JWT `sub` claim as the event organizer identifier.

#### Scenario: Client attempts to set another organizer
- **WHEN** a valid organizer includes an organizer identifier in the request body
- **THEN** the service SHALL not use that value and SHALL persist the token subject instead

### Requirement: Draft creation validates event references atomically
The service SHALL create the event and its category/image links in one transaction. It SHALL reject blank titles, unsupported age restrictions, unknown or inactive categories, and images that do not belong to the organizer without committing a partial event.

#### Scenario: Request contains an invalid category or image
- **WHEN** at least one referenced category or image does not exist, an explicitly referenced category is inactive, or an image belongs to another organizer
- **THEN** the service SHALL return a client error and SHALL not persist the event or any link

#### Scenario: Request contains invalid scalar data
- **WHEN** the title is blank or the age restriction is not one of `0`, `6`, `12`, `16`, or `18`
- **THEN** the service SHALL return `400 Bad Request` with field-level validation details

### Requirement: Event creation is idempotent
The service SHALL use `Idempotency-Key` together with the authenticated organizer and a request fingerprint to prevent duplicate draft creation.

#### Scenario: Same command is retried
- **WHEN** the same organizer repeats the request with the same `Idempotency-Key` and identical request content
- **THEN** the service SHALL return the original event result and SHALL NOT create another event

#### Scenario: Key is reused with different content
- **WHEN** the same organizer reuses an `Idempotency-Key` with different request content
- **THEN** the service SHALL return `409 Conflict`

#### Scenario: Idempotency key is missing
- **WHEN** an organizer submits the creation request without `Idempotency-Key`
- **THEN** the service SHALL return `400 Bad Request`

### Requirement: Draft creation does not publish catalog changes
The service SHALL NOT publish a Kafka event or make a draft available to the catalog as a result of `POST /api/v1/events`.

#### Scenario: Draft is created
- **WHEN** the transaction creating the draft commits
- **THEN** no catalog integration event SHALL be emitted for that draft creation

### Requirement: Event timestamps use instants
The service SHALL persist event technical timestamps as UTC instants and SHALL expose them in ISO 8601 format. Lifecycle timestamps other than creation/update SHALL be unset for a new draft.

#### Scenario: Draft response contains timestamps
- **WHEN** the service returns a newly created draft
- **THEN** `createdAt` and `updatedAt` SHALL be present
- **AND** `publishedAt`, `cancelledAt`, and `completedAt` SHALL be absent or null
