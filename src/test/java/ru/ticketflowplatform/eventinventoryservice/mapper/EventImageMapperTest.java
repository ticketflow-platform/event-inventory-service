package ru.ticketflowplatform.eventinventoryservice.mapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.CreateEventResponseDto.EventImageResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Event;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.EventImage;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Image;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.ImageType;

class EventImageMapperTest {

    private final EventImageMapper eventImageMapper = new EventImageMapperImpl();

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventImageMapperTestCases#toEntity_whenEventImageDataIsProvided_shouldMapLinkTestCases")
    @DisplayName("Преобразует событие и изображение в связь по сценариям")
    void toEntity_whenEventImageDataIsProvided_shouldMapLinkTest(
            String scenario,
            Event event,
            Image image,
            ImageType type,
            Instant now,
            UUID eventId,
            UUID imageId
    ) {
        EventImage link = eventImageMapper.toEntity(event, image, type, now);

        Assertions.assertEquals(eventId, link.getEventImageId().getEventId());
        Assertions.assertEquals(imageId, link.getEventImageId().getImageId());
        Assertions.assertSame(event, link.getEvent());
        Assertions.assertSame(image, link.getImage());
        Assertions.assertEquals(type, link.getType());
        Assertions.assertEquals(now, link.getCreatedAt());
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventImageMapperTestCases#toResponse_whenLinkIsProvided_shouldMapResponseTestCases")
    @DisplayName("Преобразует связь изображения в ответ по сценариям")
    void toResponse_whenLinkIsProvided_shouldMapResponseTest(
            String scenario,
            EventImage link,
            EventImageResponseDto expected
    ) {
        Assertions.assertEquals(expected, eventImageMapper.toResponse(link));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventImageMapperTestCases#toResponses_whenLinksAreProvided_shouldMapResponsesTestCases")
    @DisplayName("Преобразует список связей изображений по сценариям")
    void toResponses_whenLinksAreProvided_shouldMapResponsesTest(
            String scenario,
            List<EventImage> links,
            List<EventImageResponseDto> expected
    ) {
        Assertions.assertEquals(expected, eventImageMapper.toResponses(links));
    }
}
