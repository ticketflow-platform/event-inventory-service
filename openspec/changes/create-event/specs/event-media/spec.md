## ADDED Requirements

### Requirement: Event supports reusable images
An event MAY have zero or more reusable image records through event-image links when created. An image MAY be linked to multiple events owned by the same organizer. A future publication workflow SHALL require a cover image.

#### Scenario: Draft references existing images
- **WHEN** a valid request contains existing image IDs
- **THEN** the service SHALL create links between the event and those images
- **AND** the request SHALL not contain binary file content

#### Scenario: Draft has no images
- **WHEN** a draft request omits `images`, sets it to `null`, or supplies an empty array
- **THEN** the service SHALL create the draft without image links

### Requirement: Draft image references have one cover
When a draft request supplies images, exactly one image SHALL have type `COVER`; any number of images MAY have type `GALLERY`. A draft MAY be created without images.

#### Scenario: Draft images contain exactly one cover
- **WHEN** a request contains one `COVER` image and zero or more `GALLERY` images
- **THEN** the service SHALL create all supplied event-image links

#### Scenario: Draft images omit or duplicate the cover
- **WHEN** a non-empty image list contains no `COVER` or more than one `COVER`
- **THEN** the service SHALL return `400 Bad Request` and SHALL NOT create the event or image links

### Requirement: Image links retain display position
An event-image link SHALL support `ImagePosition` values `COVER` and `GALLERY`. Request and response image objects SHALL expose this value as `position`. In the create response, the `COVER` image SHALL appear before all `GALLERY` images.

#### Scenario: Image is associated with an event
- **WHEN** an image is linked to an event
- **THEN** the relationship SHALL retain its position independently of other events using the same image

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
