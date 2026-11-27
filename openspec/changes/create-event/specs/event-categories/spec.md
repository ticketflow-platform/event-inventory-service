## ADDED Requirements

### Requirement: Event supports multiple administrator-managed categories
An event MAY have zero or more active categories when created as a draft. Categories SHALL be reusable across events and SHALL be managed outside the event creation request. A future publication workflow SHALL require at least one category.

#### Scenario: Draft has multiple categories
- **WHEN** a valid request contains multiple active category IDs
- **THEN** the service SHALL associate every referenced category with the event

#### Scenario: Draft has no categories
- **WHEN** a draft request omits `categoryIds`, sets it to `null`, or supplies an empty array
- **THEN** the service SHALL create the draft without category links

### Requirement: Only active categories can be attached
The service SHALL reject references to categories that do not exist or are inactive.

#### Scenario: Inactive category is referenced
- **WHEN** a request contains an inactive category ID
- **THEN** the service SHALL reject the request and SHALL not create the event
