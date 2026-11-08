package ru.ticketflowplatform.eventinventoryservice.scenario.cases;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto.CategoryRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto.ImageRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.CreateEventResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Category;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Event;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.EventCategory;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.EventImage;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Image;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.EventStatus;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.ImageType;

public final class EventMapperTestCases {

    public static Stream<Arguments> toEntity_whenRequestHasEventData_shouldMapEventFieldsTestCases() {
        Instant now = Instant.parse("2026-01-02T03:04:05Z");
        return Stream.of(
                Arguments.of("Полные данные", new CreateEventRequestDto(
                                "Concert", "Live music", List.of(new CategoryRequestDto(UUID.fromString("00000000-0000-0000-0000-000000000001"))),
                                List.of(new ImageRequestDto(
                                        UUID.fromString("00000000-0000-0000-0000-000000000002"), ImageType.COVER
                                )), 16
                        ),
                        "organizer-1", 16, now, "Concert", "Live music"),
                Arguments.of("Без описания", new CreateEventRequestDto(
                                "Cinema", null, List.of(new CategoryRequestDto(UUID.fromString("00000000-0000-0000-0000-000000000001"))),
                                List.of(new ImageRequestDto(
                                        UUID.fromString("00000000-0000-0000-0000-000000000002"), ImageType.COVER
                                )), 0
                        ),
                        "organizer-2", 0, now, "Cinema", null)
        );
    }

    public static Stream<Arguments> toResponse_whenEventHasLinks_shouldMapResponseTestCases() {
        UUID eventId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID categoryId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID imageId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        Instant now = Instant.parse("2026-01-02T03:04:05Z");
        Category category = new Category();
        category.setId(categoryId);
        Image image = new Image();
        image.setId(imageId);
        Event event = new Event();
        event.setId(eventId);
        event.setOrganizerId("organizer-1");
        event.setTitle("Concert");
        event.setDescription("Live music");
        event.setAgeRestriction(16);
        event.setStatus(EventStatus.DRAFT);
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        event.setEventCategories(new ArrayList<>(List.of(new EventCategory(
                new EventCategory.EventCategoryId(eventId, categoryId), now, event, category
        ))));
        event.setEventImages(new ArrayList<>(List.of(new EventImage(
                new EventImage.EventImageId(eventId, imageId), ImageType.COVER, now, event, image
        ))));
        CreateEventResponseDto expected = new CreateEventResponseDto(
                eventId, "organizer-1", "Concert", "Live music", List.of(categoryId),
                List.of(new CreateEventResponseDto.EventImageResponseDto(imageId, ImageType.COVER, now)),
                16, EventStatus.DRAFT, now, now, null, null, null
        );

        return Stream.of(Arguments.of("Категория и обложка", event, expected));
    }
}
