package ru.ticketflowplatform.eventinventoryservice.mapper;

import java.time.Instant;
import org.junit.jupiter.api.Assertions;
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
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventMapperTestCases#toEntity_whenRequestHasEventData_shouldMapEventFieldsTestCases")
    @DisplayName("Преобразует запрос в событие по сценариям")
    void toEntity_whenRequestHasEventData_shouldMapEventFieldsTest(
            String scenario,
            CreateEventRequestDto request,
            String organizerId,
            Integer ageRestriction,
            Instant now,
            String expectedTitle,
            String expectedDescription
    ) {
        Event event = eventMapper.toEntity(request, organizerId, ageRestriction, now);

        Assertions.assertNull(event.getId());
        Assertions.assertEquals(organizerId, event.getOrganizerId());
        Assertions.assertEquals(expectedTitle, event.getTitle());
        Assertions.assertEquals(expectedDescription, event.getDescription());
        Assertions.assertEquals(ageRestriction, event.getAgeRestriction());
        Assertions.assertEquals(EventStatus.DRAFT, event.getStatus());
        Assertions.assertEquals(now, event.getCreatedAt());
        Assertions.assertEquals(now, event.getUpdatedAt());
        Assertions.assertTrue(event.getEventCategories().isEmpty());
        Assertions.assertTrue(event.getEventImages().isEmpty());
        Assertions.assertNull(event.getPublishedAt());
        Assertions.assertNull(event.getCancelledAt());
        Assertions.assertNull(event.getCompletedAt());
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventMapperTestCases#toResponse_whenEventHasLinks_shouldMapResponseTestCases")
    @DisplayName("Преобразует событие в ответ по сценариям")
    void toResponse_whenEventHasLinks_shouldMapResponseTest(
            String scenario,
            Event event,
            CreateEventResponseDto expected
    ) {
        Assertions.assertEquals(expected, eventMapper.toResponse(event));
    }
}
