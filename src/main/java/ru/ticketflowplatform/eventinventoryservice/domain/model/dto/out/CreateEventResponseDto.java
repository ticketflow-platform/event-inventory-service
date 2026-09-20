package ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out;

import java.time.Instant;
import java.util.UUID;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.EventStatus;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.ImageType;

public record CreateEventResponseDto(
        UUID id,
        UUID organizerId,
        String title,
        String description,
        Integer ageRestriction,
        EventStatus status,
        Instant createdAt
) {
    public record EventCategoryResponseDto(
            UUID categoryId,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record EventImageResponseDto(
            UUID imageId,
            ImageType type,
            Instant createdAt
    ) {
    }
}
