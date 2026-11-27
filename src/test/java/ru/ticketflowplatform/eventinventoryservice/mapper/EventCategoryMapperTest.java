package ru.ticketflowplatform.eventinventoryservice.mapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Category;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Event;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.EventCategory;

class EventCategoryMapperTest {

    private final EventCategoryMapper eventCategoryMapper = new EventCategoryMapperImpl();

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.mapper.EventCategoryMapperTestCases#toEntity_whenEventAndCategoryAreProvided_thenMapLinkTestCases")
    @DisplayName("Преобразует событие и категорию в связь по сценариям")
    void toEntity_whenEventAndCategoryAreProvided_thenMapLinkTest(
        String scenario,
        Event event,
        Category category,
        Instant now,
        EventCategory.EventCategoryId expectedId) {
            EventCategory link = eventCategoryMapper.toEntity(event, category, now);

            Assertions.assertThat(link.getEventCategoryId()).isEqualTo(expectedId);
            Assertions.assertThat(link.getEvent()).isSameAs(event);
            Assertions.assertThat(link.getCategory()).isSameAs(category);
            Assertions.assertThat(link.getCreatedAt()).isEqualTo(now);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.mapper.EventCategoryMapperTestCases#toEntity_whenAllInputsAreNull_thenReturnNullTestCases")
    @DisplayName("Возвращает null, если все аргументы преобразования отсутствуют")
    void toEntity_whenAllInputsAreNull_thenReturnNullTest(String scenario) {
            Assertions.assertThat(eventCategoryMapper.toEntity(null, null, null)).isNull();
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.mapper.EventCategoryMapperTestCases#toCategoryId_whenLinkIsProvided_thenReturnCategoryIdTestCases")
    @DisplayName("Извлекает идентификатор категории по сценариям")
    void toCategoryId_whenLinkIsProvided_thenReturnCategoryIdTest(
        String scenario, EventCategory link, UUID categoryId) {
            Assertions.assertThat(eventCategoryMapper.toCategoryId(link)).isEqualTo(categoryId);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.mapper.EventCategoryMapperTestCases#toCategoryIds_whenLinksAreProvided_thenReturnCategoryIdsTestCases")
    @DisplayName("Извлекает идентификаторы категорий по сценариям")
    void toCategoryIds_whenLinksAreProvided_thenReturnCategoryIdsTest(
        String scenario, List<EventCategory> links, List<UUID> categoryIds) {
            Assertions.assertThat(eventCategoryMapper.toCategoryIds(links)).isEqualTo(categoryIds);
    }
}
