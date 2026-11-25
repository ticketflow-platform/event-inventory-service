package scenario.cases.java.mapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Category;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Event;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.EventCategory;

public final class EventCategoryMapperTestCases {

    public static Stream<Arguments> toEntity_whenEventAndCategoryAreProvided_thenMapLinkTestCases() {
        UUID eventId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID categoryId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Event event = new Event();
        event.setId(eventId);
        Category category = new Category();
        category.setId(categoryId);
        Instant now = Instant.parse("2026-01-02T03:04:05Z");
        return Stream.of(
            Arguments.of(
                "Существующая категория",
                event,
                category,
                now,
                new EventCategory.EventCategoryId(eventId, categoryId)
            ),
            Arguments.of(
                "Только событие",
                event,
                null,
                now,
                new EventCategory.EventCategoryId(eventId, null)
            ),
            Arguments.of(
                "Только категория",
                null,
                category,
                now,
                new EventCategory.EventCategoryId(null, categoryId)
            ),
            Arguments.of("Только время", null, null, now, null)
        );
    }

    public static Stream<Arguments> toEntity_whenAllInputsAreNull_thenReturnNullTestCases() {
        return Stream.of(Arguments.of("Все аргументы отсутствуют"));
    }

    public static Stream<Arguments> toCategoryId_whenLinkIsProvided_thenReturnCategoryIdTestCases() {
        UUID categoryId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Category category = new Category();
        category.setId(categoryId);
        EventCategory link = new EventCategory();
        link.setCategory(category);
        return Stream.of(Arguments.of("Связь с категорией", link, categoryId));
    }

    public static Stream<Arguments> toCategoryIds_whenLinksAreProvided_thenReturnCategoryIdsTestCases() {
        UUID categoryId1 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID categoryId2 = UUID.fromString("00000000-0000-0000-0000-000000000003");
        Category category1 = new Category();
        category1.setId(categoryId1);
        Category category2 = new Category();
        category2.setId(categoryId2);
        EventCategory link1 = new EventCategory();
        link1.setCategory(category1);
        EventCategory link2 = new EventCategory();
        link2.setCategory(category2);
        return Stream.of(
            Arguments.of("Несколько связей", List.of(link1, link2), List.of(categoryId1, categoryId2)),
            Arguments.of("Нет связей", List.of(), List.of()),
            Arguments.of("Список связей отсутствует", null, null)
        );
    }
}
