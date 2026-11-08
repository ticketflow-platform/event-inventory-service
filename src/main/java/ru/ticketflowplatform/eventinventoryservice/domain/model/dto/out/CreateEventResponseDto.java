package ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.EventStatus;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.ImageType;

public record CreateEventResponseDto(
        UUID id,
        String organizerId,
        String title,
        String description,
        List<UUID> categoryIds,
        List<EventImageResponseDto> images,
        Integer ageRestriction,
        EventStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt,
        Instant cancelledAt,
        Instant completedAt
) {
    public record EventImageResponseDto(
            UUID imageId,
            ImageType type,
            Instant createdAt
    ) {
    }
}
