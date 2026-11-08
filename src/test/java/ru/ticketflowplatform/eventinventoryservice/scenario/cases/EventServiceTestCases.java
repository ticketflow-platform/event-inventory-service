package ru.ticketflowplatform.eventinventoryservice.scenario.cases;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto.CategoryRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto.ImageRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.CreateEventResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Category;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Event;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.IdempotencyKey;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Image;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.EventStatus;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.ImageType;

public final class EventServiceTestCases {

    public static Stream<Arguments> create_whenStoredKeyMatchesRequest_shouldReturnStoredResponseTestCases() {
        UUID categoryId = new UUID(0, 2);
        UUID imageId = new UUID(0, 3);
        CreateEventRequestDto request = new CreateEventRequestDto(
                "Concert", null, List.of(new CategoryRequestDto(categoryId)), List.of(new ImageRequestDto(imageId, ImageType.COVER)), 0
        );
        Event event = new Event();
        event.setId(new UUID(0, 1));
        event.setOrganizerId("organizer-1");
        event.setTitle("Concert");
        event.setAgeRestriction(0);
        event.setStatus(EventStatus.DRAFT);
        Instant now = Instant.parse("2026-01-02T03:04:05Z");
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        IdempotencyKey key = new IdempotencyKey();
        key.setRequestHash("fingerprint");
        key.setEvent(event);
        CreateEventResponseDto response = new CreateEventResponseDto(
                event.getId(), "organizer-1", "Concert", null, List.of(categoryId),
                List.of(new CreateEventResponseDto.EventImageResponseDto(imageId, ImageType.COVER, now)), 0,
                EventStatus.DRAFT, now, now, null, null, null
        );
        return Stream.of(Arguments.of("Идемпотентный повтор", request, key, response));
    }

    public static Stream<Arguments> create_whenStoredKeyHasDifferentFingerprint_shouldThrowConflictTestCases() {
        CreateEventRequestDto request = new CreateEventRequestDto(
                "Concert", null, List.of(new CategoryRequestDto(new UUID(0, 2))),
                List.of(new ImageRequestDto(new UUID(0, 3), ImageType.COVER)), 0
        );
        IdempotencyKey key = new IdempotencyKey();
        key.setRequestHash("previous-fingerprint");
        return Stream.of(Arguments.of("Другой отпечаток запроса", request, key));
    }

    public static Stream<Arguments> create_whenAgeRestrictionIsUnsupported_shouldRejectRequestTestCases() {
        UUID categoryId = new UUID(0, 2);
        List<ImageRequestDto> images = List.of(new ImageRequestDto(new UUID(0, 3), ImageType.COVER));
        return Stream.of(
                Arguments.of("Отрицательное значение", new CreateEventRequestDto("Concert", null, List.of(new CategoryRequestDto(categoryId)), images, -1)),
                Arguments.of("Значение между поддерживаемыми", new CreateEventRequestDto("Concert", null, List.of(new CategoryRequestDto(categoryId)), images, 7)),
                Arguments.of("Значение выше поддерживаемого", new CreateEventRequestDto("Concert", null, List.of(new CategoryRequestDto(categoryId)), images, 20))
        );
    }

    public static Stream<Arguments> create_whenCategoryIdsContainDuplicates_shouldRejectRequestTestCases() {
        UUID categoryId = new UUID(0, 1);
        return Stream.of(Arguments.of(
                "Один идентификатор указан дважды",
                new CreateEventRequestDto(
                        "Concert", null, List.of(new CategoryRequestDto(categoryId), new CategoryRequestDto(categoryId)),
                        List.of(new ImageRequestDto(new UUID(0, 3), ImageType.COVER)), 0
                )
        ));
    }

    public static Stream<Arguments> create_whenImageIdsContainDuplicates_shouldRejectRequestTestCases() {
        UUID imageId = new UUID(0, 1);
        ImageRequestDto image = new ImageRequestDto(imageId, ImageType.COVER);
        return Stream.of(Arguments.of(
                "Одно изображение указано дважды",
                new CreateEventRequestDto("Concert", null, List.of(new CategoryRequestDto(new UUID(0, 2))), List.of(image, image), 0)
        ));
    }

    public static Stream<Arguments> create_whenCategoryIsMissingOrInactive_shouldRejectRequestTestCases() {
        UUID categoryId = new UUID(0, 1);
        CreateEventRequestDto request = new CreateEventRequestDto(
                "Concert", null, List.of(new CategoryRequestDto(categoryId)),
                List.of(new ImageRequestDto(new UUID(0, 3), ImageType.COVER)), 0
        );
        return Stream.of(
                Arguments.of("Категория неизвестна", request, List.of()),
                Arguments.of("Активная категория не найдена", request, List.of())
        );
    }

    public static Stream<Arguments> create_whenImageIsMissingOrForeign_shouldRejectRequestTestCases() {
        UUID imageId = new UUID(0, 1);
        CreateEventRequestDto request = new CreateEventRequestDto(
                "Concert", null, List.of(new CategoryRequestDto(new UUID(0, 2))), List.of(new ImageRequestDto(imageId, ImageType.COVER)), 0
        );
        return Stream.of(
                Arguments.of("Изображение неизвестно", request, List.of()),
                Arguments.of("Изображение принадлежит другому организатору", request, List.of())
        );
    }

    public static Stream<Arguments> create_whenRequiredReferencesAreMissing_shouldRejectRequestTestCases() {
        return Stream.of(
                Arguments.of("Пустой черновик", new CreateEventRequestDto("Concert", null, null, null, null))
        );
    }

    public static Stream<Arguments> create_whenReferencesAreValid_shouldPersistAllLinksInOneTransactionTestCases() {
        Instant now = Instant.parse("2026-01-02T03:04:05Z");
        List<Category> categories = IntStream.range(0, 100)
                .mapToObj(index -> {
                    Category category = new Category();
                    category.setId(new UUID(1, index));
                    category.setName("Category " + index);
                    category.setStatus(ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.CategoryStatus.ACTIVE);
                    return category;
                })
                .toList();
        List<UUID> categoryIds = categories.stream().map(Category::getId).toList();
        UUID imageId = new UUID(2, 1);
        Image image = new Image();
        image.setId(imageId);
        image.setOrganizerId("organizer-1");
        ImageRequestDto imageRequest = new ImageRequestDto(imageId, ImageType.COVER);
        CreateEventRequestDto populatedRequest = new CreateEventRequestDto(
                "Festival", "Live", categories.stream().map(category -> new CategoryRequestDto(category.getId())).toList(), List.of(imageRequest), 16
        );
        Event populatedEvent = new Event();
        populatedEvent.setId(new UUID(0, 1001));
        populatedEvent.setEventCategories(new java.util.ArrayList<>());
        populatedEvent.setEventImages(new java.util.ArrayList<>());
        CreateEventResponseDto populatedResponse = new CreateEventResponseDto(
                populatedEvent.getId(), "organizer-1", "Festival", "Live", categoryIds,
                List.of(new CreateEventResponseDto.EventImageResponseDto(imageId, ImageType.COVER, now)),
                16, EventStatus.DRAFT, now, now, null, null, null
        );

        return Stream.of(Arguments.of(
                "Сто категорий и одно изображение",
                populatedRequest,
                populatedEvent,
                16,
                categories,
                List.of(image),
                categories.size(),
                1,
                populatedResponse
        ));
    }
}
