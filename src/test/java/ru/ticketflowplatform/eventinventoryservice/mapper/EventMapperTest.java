package ru.ticketflowplatform.eventinventoryservice.mapper;

import java.time.Instant;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.CreateEventResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Event;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.EventStatus;

class EventMapperTest {

    private final EventMapper eventMapper = new EventMapperImpl(
        new EventCategoryMapperImpl(),
        new EventImageMapperImpl()
    );

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.mapper.EventMapperTestCases#toEntity_whenRequestHasEventData_thenMapEventFieldsTestCases")
    @DisplayName("Преобразует запрос в событие по сценариям")
    void toEntity_whenRequestHasEventData_thenMapEventFieldsTest(
        String scenario,
        CreateEventRequestDto request,
        String organizerId,
        Integer ageRestriction,
        Instant now,
        String expectedTitle,
        String expectedDescription) {
        Event event = eventMapper.toEntity(request, organizerId, ageRestriction, now);

        Assertions.assertThat(event.getId()).isNull();
        Assertions.assertThat(event.getOrganizerId()).isEqualTo(organizerId);
        Assertions.assertThat(event.getTitle()).isEqualTo(expectedTitle);
        Assertions.assertThat(event.getDescription()).isEqualTo(expectedDescription);
        Assertions.assertThat(event.getAgeRestriction()).isEqualTo(ageRestriction);
        Assertions.assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
        Assertions.assertThat(event.getCreatedAt()).isEqualTo(now);
        Assertions.assertThat(event.getUpdatedAt()).isEqualTo(now);
        Assertions.assertThat(event.getEventCategories()).isEmpty();
        Assertions.assertThat(event.getEventImages()).isEmpty();
        Assertions.assertThat(event.getPublishedAt()).isNull();
        Assertions.assertThat(event.getCancelledAt()).isNull();
        Assertions.assertThat(event.getCompletedAt()).isNull();
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.mapper.EventMapperTestCases#toEntity_whenOptionalInputsAreMissing_thenMapAvailableValuesTestCases")
    @DisplayName("Преобразует доступные значения при частично отсутствующих аргументах")
    void toEntity_whenOptionalInputsAreMissing_thenMapAvailableValuesTest(
        String scenario,
        CreateEventRequestDto request,
        String organizerId,
        Integer ageRestriction,
        Instant now,
        String expectedTitle,
        String expectedDescription,
        String expectedOrganizerId,
        Integer expectedAgeRestriction) {
        Event event = eventMapper.toEntity(request, organizerId, ageRestriction, now);

        Assertions.assertThat(event.getId()).isNull();
        Assertions.assertThat(event.getOrganizerId()).isEqualTo(expectedOrganizerId);
        Assertions.assertThat(event.getTitle()).isEqualTo(expectedTitle);
        Assertions.assertThat(event.getDescription()).isEqualTo(expectedDescription);
        Assertions.assertThat(event.getAgeRestriction()).isEqualTo(expectedAgeRestriction);
        Assertions.assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
        Assertions.assertThat(event.getCreatedAt()).isEqualTo(now);
        Assertions.assertThat(event.getUpdatedAt()).isEqualTo(now);
        Assertions.assertThat(event.getEventCategories()).isEmpty();
        Assertions.assertThat(event.getEventImages()).isEmpty();
        Assertions.assertThat(event.getPublishedAt()).isNull();
        Assertions.assertThat(event.getCancelledAt()).isNull();
        Assertions.assertThat(event.getCompletedAt()).isNull();
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.mapper.EventMapperTestCases#toEntity_whenAllInputsAreNull_thenReturnNullTestCases")
    @DisplayName("Возвращает null, если все аргументы преобразования отсутствуют")
    void toEntity_whenAllInputsAreNull_thenReturnNullTest(String scenario) {
        Assertions.assertThat(eventMapper.toEntity(null, null, null, null)).isNull();
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.mapper.EventMapperTestCases#toResponse_whenEventHasLinks_thenMapResponseTestCases")
    @DisplayName("Преобразует событие в ответ по сценариям")
    void toResponse_whenEventHasLinks_thenMapResponseTest(
        String scenario,
        Event event,
        CreateEventResponseDto expected) {
        Assertions.assertThat(eventMapper.toResponse(event)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.mapper.EventMapperTestCases#toResponse_whenEventIsMissing_thenReturnNullTestCases")
    @DisplayName("Возвращает null для отсутствующего события")
    void toResponse_whenEventIsMissing_thenReturnNullTest(String scenario, Event event) {
        Assertions.assertThat(eventMapper.toResponse(event)).isNull();
    }
}
