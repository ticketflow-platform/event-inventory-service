package scenario.cases.java.service;

import java.time.Instant;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto.CategoryRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto.ImageRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.CreateEventResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Category;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Event;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.EventImage;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.IdempotencyKey;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Image;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.EventStatus;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.CategoryStatus;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.ImagePosition;

public final class EventServiceTestCases {

    public static Stream<Arguments> create_whenStoredImagesAreUnordered_thenReturnOrderedResponseTestCases() {
        Instant now = Instant.parse("2026-01-02T03:04:05Z");
        UUID eventId = new UUID(0, 1);
        Image cover = new Image();
        cover.setId(new UUID(0, 2));
        Image gallery = new Image();
        gallery.setId(new UUID(0, 3));
        Image otherGallery = new Image();
        otherGallery.setId(new UUID(0, 4));
        Event event = new Event();
        event.setId(eventId);
        event.setOrganizerId("organizer-1");
        event.setTitle("Concert");
        event.setStatus(EventStatus.DRAFT);
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        event.setEventImages(
            new ArrayList<>(
                List.of(
                    new EventImage(
                        new EventImage.EventImageId(eventId, otherGallery.getId()),
                        ImagePosition.GALLERY,
                        now,
                        event,
                        otherGallery
                    ),
                    new EventImage(new EventImage.EventImageId(eventId, cover.getId()), ImagePosition.COVER, now, event, cover),
                    new EventImage(new EventImage.EventImageId(eventId, gallery.getId()), ImagePosition.GALLERY, now, event, gallery)
                )
            )
        );
        IdempotencyKey key = new IdempotencyKey();
        key.setRequestHash("fingerprint");
        key.setEvent(event);
        CreateEventRequestDto request = new CreateEventRequestDto(
            "Concert",
            null,
            null,
            List.of(
                new ImageRequestDto(otherGallery.getId(), ImagePosition.GALLERY),
                new ImageRequestDto(cover.getId(), ImagePosition.COVER),
                new ImageRequestDto(gallery.getId(), ImagePosition.GALLERY)
            ),
            null
        );
        CreateEventResponseDto expected = new CreateEventResponseDto(
            eventId,
            "organizer-1",
            "Concert",
            null,
            List.of(),
            List.of(
                new CreateEventResponseDto.EventImageResponseDto(cover.getId(), ImagePosition.COVER, now),
                new CreateEventResponseDto.EventImageResponseDto(gallery.getId(), ImagePosition.GALLERY, now),
                new CreateEventResponseDto.EventImageResponseDto(otherGallery.getId(), ImagePosition.GALLERY, now)
            ),
            null,
            EventStatus.DRAFT,
            now,
            now,
            null,
            null,
            null
        );
        return Stream.of(Arguments.of("Обложка первая, галерея отсортирована по id", request, key, expected));
    }

    public static Stream<Arguments> create_whenStoredKeyMatchesRequest_thenReturnStoredResponseTestCases() {
        UUID categoryId = new UUID(0, 2);
        UUID imageId = new UUID(0, 3);
        CreateEventRequestDto request = new CreateEventRequestDto(
            "Concert",
            null,
            List.of(new CategoryRequestDto(categoryId)),
            List.of(new ImageRequestDto(imageId, ImagePosition.COVER)),
            0
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
            event.getId(),
            "organizer-1",
            "Concert",
            null,
            List.of(categoryId),
            List.of(new CreateEventResponseDto.EventImageResponseDto(imageId, ImagePosition.COVER, now)),
            0,
            EventStatus.DRAFT,
            now,
            now,
            null,
            null,
            null
        );
        return Stream.of(Arguments.of("Идемпотентный повтор", request, key, response));
    }

    public static Stream<Arguments> create_whenStoredKeyReferencesMissingEvent_thenThrowNotFoundTestCases() {
        UUID eventId = new UUID(0, 1);
        CreateEventRequestDto request = new CreateEventRequestDto("Concert", null, null, null, null);
        Event event = new Event();
        event.setId(eventId);
        IdempotencyKey key = new IdempotencyKey();
        key.setRequestHash("fingerprint");
        key.setKey("request-1");
        key.setEvent(event);
        return Stream.of(Arguments.of("Событие по ключу удалено", request, key, eventId));
    }

    public static Stream<Arguments> create_whenStoredKeyHasDifferentFingerprint_thenThrowConflictTestCases() {
        CreateEventRequestDto request = new CreateEventRequestDto(
            "Concert",
            null,
            List.of(new CategoryRequestDto(new UUID(0, 2))),
            List.of(new ImageRequestDto(new UUID(0, 3), ImagePosition.COVER)),
            0
        );
        IdempotencyKey key = new IdempotencyKey();
        key.setRequestHash("previous-fingerprint");
        return Stream.of(Arguments.of("Другой отпечаток запроса", request, key));
    }

    public static Stream<Arguments> create_whenAgeRestrictionIsUnsupported_thenRejectRequestTestCases() {
        UUID categoryId = new UUID(0, 2);
        List<ImageRequestDto> images = List.of(new ImageRequestDto(new UUID(0, 3), ImagePosition.COVER));
        return Stream.of(
            Arguments.of(
                "Отрицательное значение",
                new CreateEventRequestDto("Concert", null, List.of(new CategoryRequestDto(categoryId)), images, -1),
                -1
            ),
            Arguments.of(
                "Значение между поддерживаемыми",
                new CreateEventRequestDto("Concert", null, List.of(new CategoryRequestDto(categoryId)), images, 7),
                7
            ),
            Arguments.of(
                "Значение выше поддерживаемого",
                new CreateEventRequestDto("Concert", null, List.of(new CategoryRequestDto(categoryId)), images, 20),
                20
            )
        );
    }

    public static Stream<Arguments> create_whenImageListDoesNotHaveExactlyOneCover_thenRejectRequestTestCases() {
        UUID galleryId = new UUID(0, 3);
        return Stream.of(
            Arguments.of(
                "Только галерея",
                new CreateEventRequestDto("Concert", null, null, List.of(new ImageRequestDto(galleryId, ImagePosition.GALLERY)), null)
            ),
            Arguments.of(
                "Две обложки",
                new CreateEventRequestDto(
                    "Concert",
                    null,
                    null,
                    List.of(
                        new ImageRequestDto(new UUID(0, 3), ImagePosition.COVER),
                        new ImageRequestDto(new UUID(0, 4), ImagePosition.COVER)
                    ),
                    null
                )
            )
        );
    }

    public static Stream<Arguments> create_whenCategoryIdsContainDuplicates_thenRejectRequestTestCases() {
        UUID categoryId = new UUID(0, 1);
        return Stream.of(
            Arguments.of(
                "Один идентификатор указан дважды",
                new CreateEventRequestDto(
                    "Concert",
                    null,
                    List.of(new CategoryRequestDto(categoryId), new CategoryRequestDto(categoryId)),
                    List.of(new ImageRequestDto(new UUID(0, 3), ImagePosition.COVER)),
                    0
                )
            )
        );
    }

    public static Stream<Arguments> create_whenImageIdsContainDuplicates_thenRejectRequestTestCases() {
        UUID imageId = new UUID(0, 1);
        ImageRequestDto image = new ImageRequestDto(imageId, ImagePosition.COVER);
        return Stream.of(
            Arguments.of(
                "Одно изображение указано дважды",
                new CreateEventRequestDto("Concert", null, List.of(new CategoryRequestDto(new UUID(0, 2))), List.of(image, image), 0)
            )
        );
    }

    public static Stream<Arguments> create_whenCategoryIsMissingOrInactive_thenRejectRequestTestCases() {
        UUID categoryId = new UUID(0, 1);
        CreateEventRequestDto request = new CreateEventRequestDto(
            "Concert",
            null,
            List.of(new CategoryRequestDto(categoryId)),
            List.of(new ImageRequestDto(new UUID(0, 3), ImagePosition.COVER)),
            0
        );
        CreateEventRequestDto requestWithMultipleCategories = new CreateEventRequestDto(
            "Concert",
            null,
            List.of(new CategoryRequestDto(categoryId), new CategoryRequestDto(new UUID(0, 2))),
            request.images(),
            0
        );
        return Stream.of(
            Arguments.of("Категория неизвестна", request, List.of(), "api.event.categories.not_found.message.one"),
            Arguments.of("Активная категория не найдена", request, List.of(), "api.event.categories.not_found.message.one"),
            Arguments.of(
                "Несколько категорий неизвестны",
                requestWithMultipleCategories,
                List.of(),
                "api.event.categories.not_found.message.many"
            )
        );
    }

    public static Stream<Arguments> create_whenImageIsMissingOrForeign_thenRejectRequestTestCases() {
        UUID imageId = new UUID(0, 1);
        UUID categoryId = new UUID(0, 2);
        CreateEventRequestDto request = new CreateEventRequestDto(
            "Concert",
            null,
            List.of(new CategoryRequestDto(categoryId)),
            List.of(new ImageRequestDto(imageId, ImagePosition.COVER)),
            0
        );
        Category category = new Category();
        category.setId(categoryId);
        category.setStatus(CategoryStatus.ACTIVE);
        List<Category> foundCategories = List.of(category);
        CreateEventRequestDto requestWithMultipleImages = new CreateEventRequestDto(
            "Concert",
            null,
            request.categoryIds(),
            List.of(
                new ImageRequestDto(imageId, ImagePosition.COVER),
                new ImageRequestDto(new UUID(0, 4), ImagePosition.GALLERY)
            ),
            0
        );
        return Stream.of(
            Arguments.of(
                "Изображение неизвестно",
                request,
                foundCategories,
                List.of(),
                "api.event.images.not_found.message.one"
            ),
            Arguments.of(
                "Изображение принадлежит другому организатору",
                request,
                foundCategories,
                List.of(),
                "api.event.images.not_found.message.one"
            ),
            Arguments.of(
                "Несколько изображений неизвестны",
                requestWithMultipleImages,
                foundCategories,
                List.of(),
                "api.event.images.not_found.message.many"
            )
        );
    }

    public static Stream<Arguments> create_whenOnlyTitleIsProvided_thenCreateDraftTestCases() {
        CreateEventRequestDto request = new CreateEventRequestDto("Concert", null, null, null, null);
        Event event = new Event();
        event.setEventCategories(new ArrayList<>());
        event.setEventImages(new ArrayList<>());
        Instant now = Instant.parse("2026-01-02T03:04:05Z");
        CreateEventResponseDto expected = new CreateEventResponseDto(
            null,
            "organizer-1",
            "Concert",
            null,
            List.of(),
            List.of(),
            null,
            EventStatus.DRAFT,
            now,
            now,
            null,
            null,
            null
        );
        return Stream.of(
            Arguments.of("Черновик только с названием", request, event, expected)
        );
    }

    public static Stream<Arguments> create_whenReferencesAreValid_thenAttachAllLinksToEventTestCases() {
        Instant now = Instant.parse("2026-01-02T03:04:05Z");
        List<Category> categories = IntStream.range(0, 100)
            .mapToObj(index -> {
                Category category = new Category();
                category.setId(new UUID(1, index));
                category.setName("Category " + index);
                category.setStatus(CategoryStatus.ACTIVE);
                return category;
            })
            .toList();
        List<UUID> categoryIds = categories.stream().map(Category::getId).toList();
        UUID imageId = new UUID(2, 1);
        Image image = new Image();
        image.setId(imageId);
        image.setOrganizerId("organizer-1");
        ImageRequestDto imageRequest = new ImageRequestDto(imageId, ImagePosition.COVER);
        CreateEventRequestDto populatedRequest = new CreateEventRequestDto(
            "Festival",
            "Live",
            categories.stream().map(category -> new CategoryRequestDto(category.getId())).toList(),
            List.of(imageRequest),
            16
        );
        Event populatedEvent = new Event();
        populatedEvent.setId(new UUID(0, 1001));
        populatedEvent.setEventCategories(new java.util.ArrayList<>());
        populatedEvent.setEventImages(new java.util.ArrayList<>());
        CreateEventResponseDto populatedResponse = new CreateEventResponseDto(
            populatedEvent.getId(),
            "organizer-1",
            "Festival",
            "Live",
            categoryIds,
            List.of(new CreateEventResponseDto.EventImageResponseDto(imageId, ImagePosition.COVER, now)),
            16,
            EventStatus.DRAFT,
            now,
            now,
            null,
            null,
            null
        );

        return Stream.of(
            Arguments.of(
                "Сто категорий и одно изображение",
                populatedRequest,
                populatedEvent,
                16,
                categories,
                List.of(image),
                categories.size(),
                1,
                populatedResponse
            )
        );
    }

    public static Stream<Arguments> create_whenIdempotencyKeyUniqueConstraintConflicts_thenReturn409TestCases() {
        CreateEventRequestDto request = new CreateEventRequestDto("Concert", null, null, null, null);
        Event event = new Event();
        event.setEventCategories(new ArrayList<>());
        event.setEventImages(new ArrayList<>());
        return Stream.of(
            Arguments.of(
                "Конфликт уникальности ключа идемпотентности",
                request,
                event,
                new DataIntegrityViolationException(
                    "Constraint violation",
                    new ConstraintViolationException(
                        "Constraint violation",
                        new SQLException("Constraint violation"),
                        "uq_idempotency_organizer_key"
                    )
                ),
                409
            )
        );
    }

    public static Stream<Arguments> create_whenAnotherConstraintFails_thenRethrowDatabaseErrorTestCases() {
        CreateEventRequestDto request = new CreateEventRequestDto("Concert", null, null, null, null);
        Event event = new Event();
        event.setEventCategories(new ArrayList<>());
        event.setEventImages(new ArrayList<>());
        return Stream.of(
            Arguments.of(
                "Нарушено ограничение внешнего ключа",
                request,
                event,
                new DataIntegrityViolationException(
                    "Constraint violation",
                    new ConstraintViolationException(
                        "Constraint violation",
                        new SQLException("Constraint violation"),
                        "events_images_image_id_fkey"
                    )
                )
            )
        );
    }
}
