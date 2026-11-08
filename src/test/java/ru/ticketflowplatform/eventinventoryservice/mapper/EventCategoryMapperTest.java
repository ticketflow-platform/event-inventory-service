package ru.ticketflowplatform.eventinventoryservice.mapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Category;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Event;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.EventCategory;

class EventCategoryMapperTest {

    private final EventCategoryMapper eventCategoryMapper = new EventCategoryMapperImpl();

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventCategoryMapperTestCases#toEntity_whenEventAndCategoryAreProvided_shouldMapLinkTestCases")
    @DisplayName("Преобразует событие и категорию в связь по сценариям")
    void toEntity_whenEventAndCategoryAreProvided_shouldMapLinkTest(
            String scenario,
            Event event,
            Category category,
            Instant now,
            UUID eventId,
            UUID categoryId
    ) {
        EventCategory link = eventCategoryMapper.toEntity(event, category, now);

        Assertions.assertEquals(eventId, link.getEventCategoryId().getEventId());
        Assertions.assertEquals(categoryId, link.getEventCategoryId().getCategoryId());
        Assertions.assertSame(event, link.getEvent());
        Assertions.assertSame(category, link.getCategory());
        Assertions.assertEquals(now, link.getCreatedAt());
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventCategoryMapperTestCases#toCategoryId_whenLinkIsProvided_shouldReturnCategoryIdTestCases")
    @DisplayName("Извлекает идентификатор категории по сценариям")
    void toCategoryId_whenLinkIsProvided_shouldReturnCategoryIdTest(
            String scenario,
            EventCategory link,
            UUID categoryId
    ) {
        Assertions.assertEquals(categoryId, eventCategoryMapper.toCategoryId(link));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventCategoryMapperTestCases#toCategoryIds_whenLinksAreProvided_shouldReturnCategoryIdsTestCases")
    @DisplayName("Извлекает идентификаторы категорий по сценариям")
    void toCategoryIds_whenLinksAreProvided_shouldReturnCategoryIdsTest(
            String scenario,
            List<EventCategory> links,
            List<UUID> categoryIds
    ) {
        Assertions.assertEquals(categoryIds, eventCategoryMapper.toCategoryIds(links));
    }
}
