package ru.ticketflowplatform.eventinventoryservice.scenario.cases;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.CreateEventResponseDto.EventImageResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Event;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.EventImage;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Image;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.ImageType;

public final class EventImageMapperTestCases {

    public static Stream<Arguments> toEntity_whenEventImageDataIsProvided_shouldMapLinkTestCases() {
        UUID eventId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID imageId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Event event = new Event();
        event.setId(eventId);
        Image image = new Image();
        image.setId(imageId);
        Instant now = Instant.parse("2026-01-02T03:04:05Z");
        return Stream.of(Arguments.of("Обложка", event, image, ImageType.COVER, now, eventId, imageId));
    }

    public static Stream<Arguments> toResponse_whenLinkIsProvided_shouldMapResponseTestCases() {
        UUID imageId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Image image = new Image();
        image.setId(imageId);
        Instant now = Instant.parse("2026-01-02T03:04:05Z");
        EventImage link = new EventImage();
        link.setImage(image);
        link.setType(ImageType.GALLERY);
        link.setCreatedAt(now);
        return Stream.of(
                Arguments.of("Изображение галереи", link, new EventImageResponseDto(imageId, ImageType.GALLERY, now))
        );
    }

    public static Stream<Arguments> toResponses_whenLinksAreProvided_shouldMapResponsesTestCases() {
        UUID imageId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Image image = new Image();
        image.setId(imageId);
        Instant now = Instant.parse("2026-01-02T03:04:05Z");
        EventImage link = new EventImage();
        link.setImage(image);
        link.setType(ImageType.COVER);
        link.setCreatedAt(now);
        return Stream.of(
                Arguments.of("Одна связь", List.of(link), List.of(new EventImageResponseDto(imageId, ImageType.COVER, now))),
                Arguments.of("Пустой список", List.of(), List.of())
        );
    }
}
