## Why

The inventory service currently has no implemented contract for an organizer to create an event. The first vertical slice needs a persistent draft that requires only a title; categories, age restriction, and images can be completed before publication without exposing an incomplete event in the catalog.

## What Changes

- Add the `POST /api/v1/events` endpoint for creating an event draft.
- Authorize event creation for users with the `ORGANIZER` role.
- Derive the organizer identity from the authenticated JWT `sub` claim.
- Persist event metadata, many-to-many category links, and reusable image links.
- Require only the event title when creating a draft; allow categories, age restriction, and images to be added later.
- When images are supplied, require exactly one `COVER` and allow any number of `GALLERY` images; return the cover first.
- Make creation idempotent with an organizer-scoped `Idempotency-Key`.
- Allow only images owned by the authenticated organizer to be linked to the event.
- Keep new events in `DRAFT` status and do not publish a Kafka event for draft creation.
- Add validation and API error handling for malformed requests and unknown references.
- Keep publication-only completeness checks out of draft creation; publication will require a category, an age restriction, and a cover image.

## Capabilities

### New Capabilities

- `event-creation`: Create and persist organizer-owned event drafts through the inventory API.
- `event-media`: Associate reusable MinIO-backed images with events by image ID.
- `event-categories`: Associate one or more administrator-managed categories with events.

### Modified Capabilities

- None.

## Impact

- Adds a public HTTP API under `/api/v1/events`.
- Changes the inventory service domain and persistence model for events, categories, images, and their link entities.
- Requires JWT resource-server authorization with the `ORGANIZER` role and organizer ownership derived from `sub`.
- Requires database migrations for the event and link tables if not already present.
- Does not add Kafka publishing for draft creation.
- Does not implement MinIO upload URL generation; the event endpoint only accepts already-created image IDs.
