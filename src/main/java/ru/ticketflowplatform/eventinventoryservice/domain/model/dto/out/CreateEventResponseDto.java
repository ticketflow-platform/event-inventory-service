package ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.EventStatus;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.ImageType;

public record CreateEventResponseDto(@Schema(description = "i18n:swagger.event.response.id") UUID id,
    @Schema(description = "i18n:swagger.event.response.organizer_id") String organizerId,
    @Schema(description = "i18n:swagger.event.response.title") String title,
    @Schema(description = "i18n:swagger.event.response.description") String description,
    @Schema(description = "i18n:swagger.event.response.category_ids") List<UUID> categoryIds,
    @Schema(description = "i18n:swagger.event.response.images") List<EventImageResponseDto> images,
    @Schema(description = "i18n:swagger.event.response.age_restriction") Integer ageRestriction,
    @Schema(description = "i18n:swagger.event.response.status") EventStatus status,
    @Schema(description = "i18n:swagger.event.response.created_at") Instant createdAt,
    @Schema(description = "i18n:swagger.event.response.updated_at") Instant updatedAt,
    @Schema(description = "i18n:swagger.event.response.published_at") Instant publishedAt,
    @Schema(description = "i18n:swagger.event.response.cancelled_at") Instant cancelledAt,
    @Schema(description = "i18n:swagger.event.response.completed_at") Instant completedAt) {
    public record EventImageResponseDto(@Schema(description = "i18n:swagger.event.response.image.image_id") UUID imageId,
        @Schema(description = "i18n:swagger.event.response.image.type") ImageType type,
        @Schema(description = "i18n:swagger.event.response.image.created_at") Instant createdAt) {
    }
}
