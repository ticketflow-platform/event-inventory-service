package scenario.cases.java.mapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.CreateEventResponseDto.EventImageResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Event;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.EventImage;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Image;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.ImagePosition;

public final class EventImageMapperTestCases {

    public static Stream<Arguments> toEntity_whenLinkDataIsProvided_thenMapLinkTestCases() {
        UUID eventId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID imageId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Event event = new Event();
        event.setId(eventId);
        Image image = new Image();
        image.setId(imageId);
        Instant now = Instant.parse("2026-01-02T03:04:05Z");
        return Stream.of(
            Arguments.of(
                "Обложка",
                event,
                image,
                ImagePosition.COVER,
                now,
                new EventImage.EventImageId(eventId, imageId)
            ),
            Arguments.of(
                "Изображение галереи",
                event,
                image,
                ImagePosition.GALLERY,
                now,
                new EventImage.EventImageId(eventId, imageId)
            ),
            Arguments.of(
                "Только событие",
                event,
                null,
                ImagePosition.COVER,
                now,
                new EventImage.EventImageId(eventId, null)
            ),
            Arguments.of(
                "Только изображение",
                null,
                image,
                ImagePosition.COVER,
                now,
                new EventImage.EventImageId(null, imageId)
            ),
            Arguments.of("Только позиция", null, null, ImagePosition.GALLERY, null, null),
            Arguments.of("Только время", null, null, null, now, null)
        );
    }

    public static Stream<Arguments> toEntity_whenAllInputsAreNull_thenReturnNullTestCases() {
        return Stream.of(Arguments.of("Все аргументы отсутствуют"));
    }

    public static Stream<Arguments> toResponse_whenValidLinkIsProvided_thenMapResponseTestCases() {
        UUID imageId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Image image = new Image();
        image.setId(imageId);
        Instant now = Instant.parse("2026-01-02T03:04:05Z");
        EventImage link = new EventImage();
        link.setImage(image);
        link.setType(ImagePosition.COVER);
        link.setCreatedAt(now);
        EventImage linkWithoutImage = new EventImage();
        linkWithoutImage.setType(ImagePosition.GALLERY);
        linkWithoutImage.setCreatedAt(now);
        return Stream.of(
            Arguments.of("Обложка", link, new EventImageResponseDto(imageId, ImagePosition.COVER, now)),
            Arguments.of("Изображение не задано", linkWithoutImage, new EventImageResponseDto(null, ImagePosition.GALLERY, now)),
            Arguments.of("Связь отсутствует", null, null)
        );
    }

    public static Stream<Arguments> toResponses_whenLinksAreProvided_thenPlaceCoverFirstTestCases() {
        UUID coverId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID galleryId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID otherGalleryId = UUID.fromString("00000000-0000-0000-0000-000000000004");
        Image cover = new Image();
        cover.setId(coverId);
        Image gallery = new Image();
        gallery.setId(galleryId);
        Image otherGallery = new Image();
        otherGallery.setId(otherGalleryId);
        Instant now = Instant.parse("2026-01-02T03:04:05Z");
        EventImage coverLink = new EventImage();
        coverLink.setImage(cover);
        coverLink.setType(ImagePosition.COVER);
        coverLink.setCreatedAt(now);
        EventImage galleryLink = new EventImage();
        galleryLink.setImage(gallery);
        galleryLink.setType(ImagePosition.GALLERY);
        galleryLink.setCreatedAt(now);
        EventImage otherGalleryLink = new EventImage();
        otherGalleryLink.setImage(otherGallery);
        otherGalleryLink.setType(ImagePosition.GALLERY);
        otherGalleryLink.setCreatedAt(now);
        return Stream.of(
            Arguments.of(
                "Обложка идёт первой, галерея стабильно сортируется по id",
                List.of(otherGalleryLink, coverLink, galleryLink),
                List.of(
                    new EventImageResponseDto(coverId, ImagePosition.COVER, now),
                    new EventImageResponseDto(galleryId, ImagePosition.GALLERY, now),
                    new EventImageResponseDto(otherGalleryId, ImagePosition.GALLERY, now)
                )
            ),
            Arguments.of("Пустой список изображений", List.of(), List.of())
        );
    }
}
