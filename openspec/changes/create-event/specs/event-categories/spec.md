## ADDED Requirements

### Requirement: Event supports multiple administrator-managed categories
An event SHALL be associated with zero or more categories while it is a draft. Categories SHALL be reusable across events and SHALL be managed outside the event creation request.

#### Scenario: Draft has multiple categories
- **WHEN** a valid request contains multiple active category IDs
- **THEN** the service SHALL associate every referenced category with the event

#### Scenario: Draft has no categories
- **WHEN** a valid draft request contains no category IDs
- **THEN** the service SHALL create the draft without category links

### Requirement: Only active categories can be attached
The service SHALL reject references to categories that do not exist or are inactive.

#### Scenario: Inactive category is referenced
- **WHEN** a request contains an inactive category ID
- **THEN** the service SHALL reject the request and SHALL not create the event
