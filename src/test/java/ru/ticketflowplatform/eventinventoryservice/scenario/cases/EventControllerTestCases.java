package ru.ticketflowplatform.eventinventoryservice.scenario.cases;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public final class EventControllerTestCases {

    public static Stream<Arguments> create_whenOrganizerSubmitsRequest_shouldReturnExpectedResultTestCases() {
        return Stream.of(
                Arguments.of(
                        "Организатор создает событие",
                        "{\"title\":\"Festival\",\"description\":\"Live\",\"ageRestriction\":16,\"categoryIds\":[{\"categoryId\":\"00000000-0000-0000-0000-000000000011\"}],\"images\":[{\"imageId\":\"00000000-0000-0000-0000-000000000012\",\"type\":\"COVER\"}]}",
                        "create-success",
                        201,
                        1,
                        "\"status\":\"DRAFT\""
                ),
                Arguments.of(
                        "Название события пустое",
                        "{\"title\":\" \",\"categoryIds\":[{\"categoryId\":\"00000000-0000-0000-0000-000000000011\"}],\"images\":[{\"imageId\":\"00000000-0000-0000-0000-000000000012\",\"type\":\"COVER\"}],\"ageRestriction\":0}",
                        "create-invalid-title",
                        400,
                        0,
                        "\"field\":\"title\""
                ),
                Arguments.of(
                        "Категория отсутствует",
                        "{\"title\":\"Festival\",\"categoryIds\":[{\"categoryId\":\"00000000-0000-0000-0000-000000000099\"}],\"images\":[{\"imageId\":\"00000000-0000-0000-0000-000000000012\",\"type\":\"COVER\"}],\"ageRestriction\":0}",
                        "create-invalid-category",
                        422,
                        0,
                        "Категория с id: 00000000-0000-0000-0000-000000000099 не найдена"
                )
        );
    }

    public static Stream<Arguments> create_whenRequestIsUnauthenticated_shouldReturnUnauthorizedTestCases() {
        return Stream.of(Arguments.of(
                "Нет bearer token",
                "{\"title\":\"Festival\",\"categoryIds\":[{\"categoryId\":\"00000000-0000-0000-0000-000000000011\"}],\"images\":[{\"imageId\":\"00000000-0000-0000-0000-000000000012\",\"type\":\"COVER\"}],\"ageRestriction\":0}"
        ));
    }

    public static Stream<Arguments> create_whenUserLacksOrganizerRole_shouldReturnForbiddenTestCases() {
        return Stream.of(Arguments.of(
                "У пользователя отсутствует ORGANIZER",
                "{\"title\":\"Festival\",\"categoryIds\":[{\"categoryId\":\"00000000-0000-0000-0000-000000000011\"}],\"images\":[{\"imageId\":\"00000000-0000-0000-0000-000000000012\",\"type\":\"COVER\"}],\"ageRestriction\":0}"
        ));
    }

    public static Stream<Arguments> create_whenIdempotencyHeaderIsMissing_shouldReturnBadRequestTestCases() {
        return Stream.of(Arguments.of(
                "Нет обязательного заголовка",
                "{\"title\":\"Festival\",\"categoryIds\":[{\"categoryId\":\"00000000-0000-0000-0000-000000000011\"}],\"images\":[{\"imageId\":\"00000000-0000-0000-0000-000000000012\",\"type\":\"COVER\"}],\"ageRestriction\":0}"
        ));
    }

    public static Stream<Arguments> create_whenCategoryIdsAreMissingOrEmpty_shouldReturnBadRequestTestCases() {
        return Stream.of(
                Arguments.of("Поле не передано", "{\"title\":\"Festival\"}"),
                Arguments.of("Передан null", "{\"title\":\"Festival\",\"categoryIds\":null}"),
                Arguments.of("Передан пустой массив", "{\"title\":\"Festival\",\"categoryIds\":[]}")
        );
    }

    public static Stream<Arguments> create_whenImagesAreMissingOrEmpty_shouldReturnBadRequestTestCases() {
        return Stream.of(
                Arguments.of("Поле не передано", "{\"title\":\"Festival\"}"),
                Arguments.of("Передан null", "{\"title\":\"Festival\",\"images\":null}"),
                Arguments.of("Передан пустой массив", "{\"title\":\"Festival\",\"images\":[]}")
        );
    }

    public static Stream<Arguments> create_whenAgeRestrictionIsMissing_shouldReturnBadRequestTestCases() {
        return Stream.of(
                Arguments.of("Поле не передано", "{\"title\":\"Festival\"}"),
                Arguments.of("Передан null", "{\"title\":\"Festival\",\"ageRestriction\":null}")
        );
    }

    public static Stream<Arguments> create_whenIdempotencyHeaderIsBlank_shouldReturnBadRequestTestCases() {
        String requestBody = "{\"title\":\"Festival\",\"categoryIds\":[{\"categoryId\":\"00000000-0000-0000-0000-000000000011\"}],\"images\":[{\"imageId\":\"00000000-0000-0000-0000-000000000012\",\"type\":\"COVER\"}],\"ageRestriction\":0}";
        return Stream.of(
                Arguments.of("Значение пустое", "", requestBody),
                Arguments.of("Значение состоит из пробелов", "   ", requestBody)
        );
    }

    public static Stream<Arguments> create_whenRequestIsRetried_shouldCreateOnlyOneEventTestCases() {
        return Stream.of(Arguments.of(
                "Идентичный запрос повторен с тем же ключом",
                "{\"title\":\"Festival\",\"categoryIds\":[{\"categoryId\":\"00000000-0000-0000-0000-000000000011\"}],\"images\":[{\"imageId\":\"00000000-0000-0000-0000-000000000012\",\"type\":\"COVER\"}],\"ageRestriction\":0}",
                "create-retry"
        ));
    }

    public static Stream<Arguments> create_whenOneHundredCategoriesAreRequested_shouldPersistAllLinksTestCases() {
        String categoryIds = IntStream.rangeClosed(1, 100)
                .mapToObj("{\"categoryId\":\"00000000-0000-0000-0000-%012d\"}"::formatted)
                .collect(Collectors.joining(","));
        return Stream.of(Arguments.of(
                "Сто активных категорий",
                "{\"title\":\"Large event\",\"categoryIds\":[" + categoryIds + "],\"images\":[{\"imageId\":\"00000000-0000-0000-0000-000000000012\",\"type\":\"COVER\"}],\"ageRestriction\":0}",
                "create-hundred-categories"
        ));
    }

    public static Stream<Arguments> create_whenIdempotencyKeyIsReusedForDifferentRequest_shouldReturnConflictTestCases() {
        return Stream.of(Arguments.of(
                "Тот же ключ использован для другого названия",
                "{\"title\":\"Festival\",\"categoryIds\":[{\"categoryId\":\"00000000-0000-0000-0000-000000000011\"}],\"images\":[{\"imageId\":\"00000000-0000-0000-0000-000000000012\",\"type\":\"COVER\"}],\"ageRestriction\":0}",
                "{\"title\":\"Concert\",\"categoryIds\":[{\"categoryId\":\"00000000-0000-0000-0000-000000000011\"}],\"images\":[{\"imageId\":\"00000000-0000-0000-0000-000000000012\",\"type\":\"COVER\"}],\"ageRestriction\":0}",
                "create-conflict"
        ));
    }
}
