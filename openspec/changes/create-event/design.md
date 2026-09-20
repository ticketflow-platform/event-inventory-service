## Context

The inventory service is a Spring Boot application with JPA and PostgreSQL dependencies. Its domain model is being established for organizer-managed events, while the catalog service will later consume published event changes. A newly created event is intentionally an incomplete draft and must not be visible in the catalog.

The event owns a single organizer identified by the authenticated Keycloak JWT `sub` claim. An organizer account may represent a person, cinema, venue, company, or another event owner; the venue where an event happens is a separate domain object. Categories are administrator-managed reusable records. Images are reusable MinIO objects owned by an organizer and linked to that organizer's events through a many-to-many association; binary upload is a separate flow and is not part of event creation.

## Goals / Non-Goals

**Goals:**

- Provide `POST /api/v1/events` for creating an organizer-owned draft.
- Validate and persist event metadata and references to existing categories and images atomically.
- Enforce the `ORGANIZER` role and derive ownership from the JWT.
- Return a stable event representation with its generated identifier and `DRAFT` status.
- Keep the operation independent from Kafka and the catalog service.

**Non-Goals:**

- Publishing, editing, stopping sales, cancelling, or completing events.
- Creating sessions, venues, seats, prices, or sales periods.
- Creating categories or uploading image binaries/presigned URLs.
- Profile lookup or embedding organizer profile data in the response; a future Profile Service may own display data.
- Optimistic locking or a `version` field.

## Decisions

### HTTP contract

Use `POST /api/v1/events`. The request accepts `title`, optional `description`, optional `ageRestriction`, `categoryIds`, and `images`. Each image item contains an `imageId` and existing `ImageType` value (`COVER` or `GALLERY`); the array order is the display order. Omitted age restriction defaults to `0`, meaning no age restriction. The response includes the generated `id`, `organizerId`, supplied event data, empty or resolved category/image references, `DRAFT` status, and creation/update timestamps. `organizerId` is never accepted from the client.

The request requires an `Idempotency-Key` header. Retrying the same key with the same request returns the original result; reusing it with different request data is rejected.

### Authorization and identity

Configure the service as an OAuth2 resource server and map the Keycloak realm role `ORGANIZER` to Spring Security authority `ROLE_ORGANIZER`. Reject missing/invalid tokens with `401` and authenticated users without the role with `403`. Read `sub` from the verified JWT and persist it as a string because Keycloak subject identifiers are not required to be UUIDs.

### Persistence model

Persist `Event`, `Category`, and `Image` records with explicit link entities `EventCategory` and `EventImage`. `Image` stores its organizer owner, and `EventImage` stores the relationship type (`COVER` or `GALLERY`). The request array supplies display order, so no separate order column is required for this change. The event has one organizer and many-to-many categories/images. Do not retain a separate single `imageId` on `Event`.

### Transaction boundary

The application service performs idempotency lookup, reference checks, and event/link persistence in one transaction. Unknown category IDs, inactive categories, or images owned by another organizer fail the request and no partial event is committed. A draft is saved with status `DRAFT`. No Kafka producer or outbox record is created for this operation. Idempotency records map the organizer and key to the created event and request fingerprint.

### Validation and errors

Require a non-blank title. Default an omitted age restriction to `0` and validate it against `0`, `6`, `12`, `16`, or `18`. Validate that every referenced category is active and every referenced image exists and belongs to the organizer. Return a structured `400` for malformed data, `409` for an idempotency conflict, or a domain-level `422` for invalid references, following the existing API error convention once established.

### Time handling

Persist technical timestamps as UTC instants and expose them as ISO 8601 timestamps. Formatting for a user locale is a client concern. `publishedAt`, `cancelledAt`, and `completedAt` are lifecycle fields for later transitions and are null on a newly created draft.

## Risks / Trade-offs

- [Risk] A referenced image may have been uploaded but not actually finalized in object storage → The separate image workflow must only issue usable image IDs after upload confirmation; event creation verifies the image record.
- [Risk] Category names or profile data can change after event creation → Keep stable category IDs and organizer ID in the event; resolve display data in read models later.
- [Risk] The current project has incomplete API/security/migration scaffolding → Implement this change incrementally and cover the endpoint with focused integration tests.
- [Trade-off] Draft creation does not publish to Kafka → Catalog freshness starts only at publication, which matches the product rule that drafts are not public.

## Migration Plan

1. Add or update database migrations for events, categories, images, and link tables.
2. Deploy the API and authorization configuration.
3. Existing data is not expected for this initial service; if present, keep migration rollback limited to the new tables.

## Open Questions

- The exact structured error envelope is not yet standardized in the service.
- The presigned MinIO upload endpoint and image finalization workflow are a separate change.
