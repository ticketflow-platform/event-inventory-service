package ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in;

import java.util.List;
import java.util.UUID;

public record CreateEventRequestDto(
        String title,
        String description,
        List<EventCategoryRequestDto> categories,
        List<EventImageRequestDto> images,
        Integer ageRestriction
) {
    public record EventCategoryRequestDto(
            UUID categoryId
    ) {
    }

    public record EventImageRequestDto(
            UUID imageId
    ) {
    }
}
