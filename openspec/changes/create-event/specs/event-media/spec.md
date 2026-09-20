## ADDED Requirements

### Requirement: Event supports reusable images
An event SHALL be associated with zero or more reusable image records through event-image links. An image MAY be linked to multiple events owned by the same organizer.

#### Scenario: Draft references existing images
- **WHEN** a valid request contains existing image IDs
- **THEN** the service SHALL create links between the event and those images
- **AND** the request SHALL not contain binary file content

#### Scenario: Draft has no images
- **WHEN** a valid draft request contains no image IDs
- **THEN** the service SHALL create the draft without image links

### Requirement: Image links retain display type
An event-image link SHALL support the existing `ImageType` values `COVER` and `GALLERY`. The order of image references in the request SHALL define display order for the event.

#### Scenario: Image is associated with an event
- **WHEN** an image is linked to an event
- **THEN** the relationship SHALL retain its type independently of other events using the same image

### Requirement: Images are organizer-owned
The service SHALL allow an event to reference only images owned by the authenticated organizer.

#### Scenario: Event references another organizer's image
- **WHEN** an organizer submits an image ID owned by another organizer
- **THEN** the service SHALL reject the request and SHALL not create the event-image link

### Requirement: Binary uploads use a separate object-storage flow
The event creation endpoint SHALL accept image identifiers only. Binary upload and presigned URL generation SHALL be handled by a separate image workflow.

#### Scenario: Client creates an event after direct upload
- **WHEN** the client has uploaded an image through the separate MinIO presigned URL flow and submits its image ID with the event request
- **THEN** the event service SHALL validate the image record and create the event-image link
