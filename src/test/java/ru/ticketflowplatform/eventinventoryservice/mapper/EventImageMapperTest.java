package ru.ticketflowplatform.eventinventoryservice.mapper;

import java.time.Instant;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.CreateEventResponseDto.EventImageResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Event;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.EventImage;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Image;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.ImagePosition;

class EventImageMapperTest {

    private final EventImageMapper eventImageMapper = new EventImageMapperImpl();

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.mapper.EventImageMapperTestCases#toEntity_whenLinkDataIsProvided_thenMapLinkTestCases")
    @DisplayName("Преобразует данные обложки и галереи в связи")
    void toEntity_whenLinkDataIsProvided_thenMapLinkTest(
        String scenario,
        Event event,
        Image image,
        ImagePosition type,
        Instant now,
        EventImage.EventImageId expectedId) {
        EventImage link = eventImageMapper.toEntity(event, image, type, now);

        Assertions.assertThat(link.getEventImageId()).isEqualTo(expectedId);
        Assertions.assertThat(link.getEvent()).isSameAs(event);
        Assertions.assertThat(link.getImage()).isSameAs(image);
        Assertions.assertThat(link.getType()).isEqualTo(type);
        Assertions.assertThat(link.getCreatedAt()).isEqualTo(now);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.mapper.EventImageMapperTestCases#toEntity_whenAllInputsAreNull_thenReturnNullTestCases")
    @DisplayName("Возвращает null, если все аргументы преобразования отсутствуют")
    void toEntity_whenAllInputsAreNull_thenReturnNullTest(String scenario) {
        Assertions.assertThat(eventImageMapper.toEntity(null, null, null, null)).isNull();
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.mapper.EventImageMapperTestCases#toResponse_whenValidLinkIsProvided_thenMapResponseTestCases")
    @DisplayName("Преобразует связь изображения в ответ")
    void toResponse_whenValidLinkIsProvided_thenMapResponseTest(
        String scenario,
        EventImage link,
        EventImageResponseDto expected) {
        Assertions.assertThat(eventImageMapper.toResponse(link)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.mapper.EventImageMapperTestCases#toResponses_whenLinksAreProvided_thenPreserveOrderTestCases")
    @DisplayName("Преобразует изображения без изменения порядка")
    void toResponses_whenLinksAreProvided_thenPreserveOrderTest(
        String scenario,
        List<EventImage> links,
        List<EventImageResponseDto> expected) {
        Assertions.assertThat(eventImageMapper.toResponses(links)).isEqualTo(expected);
    }
}
