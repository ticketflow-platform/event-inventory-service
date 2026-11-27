package scenario.cases.java.controller;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.EventNotFoundException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.HashGenerationException;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.CreateEventResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.CreateEventResponseDto.EventImageResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto.FieldErrorResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.EventStatus;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.ImagePosition;

public final class EventControllerTestCases {

    public static Stream<Arguments> create_whenServiceFails_thenReturnCompleteErrorTestCases() {
            UUID eventId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            return Stream.of(
                Arguments.of(
                    "Событие из ключа отсутствует",
                    "{\"title\":\"Festival\"}",
                    "missing-event",
                    "en",
                    new EventNotFoundException(
                        "api.event.not_found.message",
                        "api.event.not_found.detail",
                        eventId,
                        "missing-event"),
                    new ErrorResponseDto(
                        404,
                        "Event not found",
                        "Event with id " + eventId + " and key missing-event was not found",
                        List.of())),
                Arguments.of(
                    "Не удалось сформировать отпечаток запроса",
                    "{\"title\":\"Festival\"}",
                    "hash-failure",
                    "ru",
                    new HashGenerationException(new NoSuchAlgorithmException("SHA-256")),
                    new ErrorResponseDto(
                        500,
                        "Ошибка обработки запроса",
                        "Не удалось сформировать контрольную сумму запроса",
                        List.of())));
    }

    public static Stream<Arguments> create_whenOrganizerSubmitsValidDraft_thenReturnCompleteResponseTestCases() {
            return Stream.of(
                Arguments.of(
                    "Обложка сортируется перед галереей, хотя передана второй",
                    "{\"title\":\"Festival\",\"description\":\"Live\",\"ageRestriction\":16,\"categoryIds\":[{\"id\":\"00000000-0000-0000-0000-000000000011\"}],\"images\":[{\"id\":\"00000000-0000-0000-0000-000000000014\",\"position\":\"GALLERY\"},{\"id\":\"00000000-0000-0000-0000-000000000012\",\"position\":\"COVER\"}]}",
                    "create-success",
                    "Festival",
                    "Live",
                    16,
                    List.of(UUID.fromString("00000000-0000-0000-0000-000000000011")),
                    List.of(
                        UUID.fromString("00000000-0000-0000-0000-000000000012"),
                        UUID.fromString("00000000-0000-0000-0000-000000000014")),
                    List.of(ImagePosition.COVER, ImagePosition.GALLERY)));
    }

    public static Stream<Arguments> create_whenOrganizerSubmitsInvalidRequest_thenReturnCompleteErrorTestCases() {
            UUID missingCategoryId = UUID.fromString("00000000-0000-0000-0000-000000000099");
            UUID inactiveCategoryId = UUID.fromString("00000000-0000-0000-0000-000000000013");
            UUID missingImageId = UUID.fromString("00000000-0000-0000-0000-000000000099");
            UUID foreignImageId = UUID.fromString("00000000-0000-0000-0000-000000000015");
            return Stream.of(
                Arguments.of(
                    "Пустое название",
                    "{\"title\":\" \",\"categoryIds\":[{\"id\":\"00000000-0000-0000-0000-000000000011\"}],\"images\":[{\"id\":\"00000000-0000-0000-0000-000000000012\",\"position\":\"COVER\"}],\"ageRestriction\":0}",
                    "create-invalid-title",
                    new ErrorResponseDto(
                        400,
                        "Некорректный запрос",
                        "Проверка запроса не пройдена",
                        List.of(
                            new FieldErrorResponseDto(
                                "title", "Название не должно быть пустым")))),
                Arguments.of(
                    "Не указана позиция изображения",
                    "{\"title\":\"Festival\",\"images\":[{\"id\":\"00000000-0000-0000-0000-000000000012\"}]}",
                    "create-missing-position",
                    new ErrorResponseDto(
                        400,
                        "Некорректный запрос",
                        "Проверка запроса не пройдена",
                        List.of(
                            new FieldErrorResponseDto("images[0].position", "Необходимо указать позицию изображения")))),
                Arguments.of(
                    "Категория отсутствует",
                    "{\"title\":\"Festival\",\"categoryIds\":[{\"id\":\"00000000-0000-0000-0000-000000000099\"}],\"images\":[{\"id\":\"00000000-0000-0000-0000-000000000012\",\"position\":\"COVER\"}],\"ageRestriction\":0}",
                    "create-invalid-category",
                    new ErrorResponseDto(
                        422,
                        "Категория не найдена",
                        "Категория с id " + missingCategoryId + " не найдена",
                        List.of())),
                Arguments.of(
                    "Категория неактивна",
                    "{\"title\":\"Festival\",\"categoryIds\":[{\"id\":\"00000000-0000-0000-0000-000000000013\"}],\"images\":[{\"id\":\"00000000-0000-0000-0000-000000000012\",\"position\":\"COVER\"}],\"ageRestriction\":0}",
                    "create-inactive-category",
                    new ErrorResponseDto(
                        422,
                        "Категория не найдена",
                        "Категория с id " + inactiveCategoryId + " не найдена",
                        List.of())),
                Arguments.of(
                    "Изображение отсутствует в PostgreSQL",
                    "{\"title\":\"Festival\",\"categoryIds\":[{\"id\":\"00000000-0000-0000-0000-000000000011\"}],\"images\":[{\"id\":\"00000000-0000-0000-0000-000000000099\",\"position\":\"COVER\"}]}",
                    "create-missing-image",
                    new ErrorResponseDto(
                        422,
                        "Изображение не найдено",
                        "Изображение с id " + missingImageId + " не найдено",
                        List.of())),
                Arguments.of(
                    "Обложка существует, но принадлежит другому организатору",
                    "{\"title\":\"Festival\",\"categoryIds\":[{\"id\":\"00000000-0000-0000-0000-000000000011\"}],\"images\":[{\"id\":\"00000000-0000-0000-0000-000000000015\",\"position\":\"COVER\"}]}",
                    "create-foreign-cover",
                    new ErrorResponseDto(
                        422,
                        "Изображение не найдено",
                        "Изображение с id " + foreignImageId + " не найдено",
                        List.of())),
                Arguments.of(
                    "Своя обложка и чужое изображение галереи не оставляют частичных данных",
                    "{\"title\":\"Festival\",\"categoryIds\":[{\"id\":\"00000000-0000-0000-0000-000000000011\"}],\"images\":[{\"id\":\"00000000-0000-0000-0000-000000000012\",\"position\":\"COVER\"},{\"id\":\"00000000-0000-0000-0000-000000000015\",\"position\":\"GALLERY\"}]}",
                    "create-foreign-gallery",
                    new ErrorResponseDto(
                        422,
                        "Изображение не найдено",
                        "Изображение с id " + foreignImageId + " не найдено",
                        List.of())));
    }

    public static Stream<Arguments> create_whenRequestIsUnauthenticated_thenReturnUnauthorizedTestCases() {
            return Stream.of(
                Arguments.of(
                    "Нет bearer token",
                    "{\"title\":\"Festival\",\"categoryIds\":[{\"id\":\"00000000-0000-0000-0000-000000000011\"}],\"images\":[{\"id\":\"00000000-0000-0000-0000-000000000012\",\"position\":\"COVER\"}],\"ageRestriction\":0}",
                    "ru",
                    new ErrorResponseDto(
                        401,
                        "Требуется аутентификация",
                        "Для доступа к ресурсу необходим действительный токен",
                        List.of())));
    }

    public static Stream<Arguments> create_whenUserLacksOrganizerRole_thenReturnForbiddenTestCases() {
            return Stream.of(
                Arguments.of(
                    "У пользователя отсутствует ORGANIZER",
                    "{\"title\":\"Festival\",\"categoryIds\":[{\"id\":\"00000000-0000-0000-0000-000000000011\"}],\"images\":[{\"id\":\"00000000-0000-0000-0000-000000000012\",\"position\":\"COVER\"}],\"ageRestriction\":0}",
                    new ErrorResponseDto(
                        403,
                        "Доступ запрещён",
                        "Недостаточно прав для выполнения операции",
                        List.of())));
    }

    public static Stream<Arguments> create_whenIdempotencyHeaderIsMissing_thenReturnLocalizedBadRequestTestCases() {
            String requestBody = "{\"title\":\"Festival\",\"categoryIds\":[{\"id\":\"00000000-0000-0000-0000-000000000011\"}],\"images\":[{\"id\":\"00000000-0000-0000-0000-000000000012\",\"position\":\"COVER\"}],\"ageRestriction\":0}";
            return Stream.of(
                Arguments.of(
                    "Русская локаль",
                    requestBody,
                    "ru",
                    new ErrorResponseDto(
                        400,
                        "Некорректный запрос",
                        "Отсутствует обязательный заголовок: Idempotency-Key",
                        List.of())),
                Arguments.of(
                    "Английская локаль",
                    requestBody,
                    "en",
                    new ErrorResponseDto(
                        400,
                        "Bad request",
                        "Missing required header: Idempotency-Key",
                        List.of())));
    }

    public static Stream<Arguments> openApi_whenLocaleIsRequested_thenDocumentSchemaAndLocalizedSpecTestCases() {
            return Stream.of(
                Arguments.of(
                    "Русская спецификация",
                    "/v3/api-docs/ru",
                    "TicketFlow — API мероприятий",
                    "Название мероприятия. Единственное обязательное поле при создании черновика.",
                    "Создать мероприятие",
                    "Внутренняя ошибка при обработке запроса"),
                Arguments.of(
                    "Английская спецификация",
                    "/v3/api-docs/en",
                    "TicketFlow Events API",
                    "Event title. The only field required to create a draft.",
                    "Create an event",
                    "Internal error while processing the request"),
                Arguments.of(
                    "Спецификация по умолчанию",
                    "/v3/api-docs",
                    "TicketFlow — API мероприятий",
                    "Название мероприятия. Единственное обязательное поле при создании черновика.",
                    "Создать мероприятие",
                    "Внутренняя ошибка при обработке запроса"));
    }

    public static Stream<Arguments> create_whenJsonIsMalformed_thenReturnBadRequestTestCases() {
            return Stream.of(
                Arguments.of(
                    "JSON не завершен",
                    "{\"title\":",
                    new ErrorResponseDto(
                        400,
                        "Некорректный запрос",
                        "Тело запроса имеет некорректный формат",
                        List.of())));
    }

    public static Stream<Arguments> health_whenRequestIsUnauthenticated_thenAllowAccessTestCases() {
            return Stream.of(
                Arguments.of(
                    "Health доступен без токена",
                    "/actuator/health",
                    200,
                    "{\"status\":\"UP\",\"groups\":[\"liveness\",\"readiness\"]}"),
                Arguments.of(
                    "Liveness доступен без токена",
                    "/actuator/health/liveness",
                    200,
                    "{\"status\":\"UP\"}"),
                Arguments.of(
                    "Readiness доступен без токена",
                    "/actuator/health/readiness",
                    200,
                    "{\"status\":\"UP\"}"));
    }

    public static Stream<Arguments> developerEndpoints_whenPathIsRequested_thenRequireDeveloperRoleTestCases() {
            return Stream.of(
                Arguments.of("Swagger UI entry", "/swagger-ui.html", 401, 403),
                Arguments.of("Swagger UI", "/swagger-ui/index.html", 401, 403),
                Arguments.of("OpenAPI", "/v3/api-docs/ru", 401, 403));
    }

    public static Stream<Arguments> create_whenBearerTokenIsInvalid_thenReturnUnauthorizedErrorTestCases() {
            return Stream.of(
                Arguments.of(
                    "Некорректный bearer-токен",
                    "invalid-token",
                    "Bearer",
                    new ErrorResponseDto(
                        401,
                        "Требуется аутентификация",
                        "Для доступа к ресурсу необходим действительный токен",
                        List.of())));
    }

    public static Stream<Arguments> create_whenOptionalDraftFieldsAreOmitted_thenCreateDraftTestCases() {
            return Stream.of(
                Arguments.of("Поля не переданы", "{\"title\":\"Festival\"}", "optional-omitted"),
                Arguments.of(
                    "Поля переданы как null",
                    "{\"title\":\"Festival\",\"description\":null,\"categoryIds\":null,\"images\":null,\"ageRestriction\":null}",
                    "optional-null"),
                Arguments.of(
                    "Коллекции категорий и изображений пусты",
                    "{\"title\":\"Festival\",\"categoryIds\":[],\"images\":[]}",
                    "optional-empty"));
    }

    public static Stream<Arguments> create_whenBusinessRuleIsViolated_thenReturnBadRequestTestCases() {
            return Stream.of(
                Arguments.of(
                    "Возрастное ограничение не поддерживается",
                    "{\"title\":\"Festival\",\"ageRestriction\":7}",
                    "invalid-age-restriction",
                    new ErrorResponseDto(
                        400,
                        "Недопустимое возрастное ограничение",
                        "Допустимые значения: 0, 6, 12, 16 и 18. Получено значение: 7",
                        List.of())),
                Arguments.of(
                    "Передана галерея без обложки",
                    "{\"title\":\"Festival\",\"images\":[{\"id\":\"00000000-0000-0000-0000-000000000014\",\"position\":\"GALLERY\"}]}",
                    "missing-cover",
                    new ErrorResponseDto(
                        400,
                        "Неверное главное изображение",
                        "Если изображения указаны, среди них должна быть ровно одна обложка типа COVER",
                        List.of())),
                Arguments.of(
                    "Переданы две обложки",
                    "{\"title\":\"Festival\",\"images\":[{\"id\":\"00000000-0000-0000-0000-000000000012\",\"position\":\"COVER\"},{\"id\":\"00000000-0000-0000-0000-000000000014\",\"position\":\"COVER\"}]}",
                    "duplicate-cover",
                    new ErrorResponseDto(
                        400,
                        "Неверное главное изображение",
                        "Если изображения указаны, среди них должна быть ровно одна обложка типа COVER",
                        List.of())));
    }

    public static Stream<Arguments> create_whenIdempotencyHeaderIsBlank_thenReturnBadRequestTestCases() {
            String requestBody = "{\"title\":\"Festival\",\"categoryIds\":[{\"id\":\"00000000-0000-0000-0000-000000000011\"}],\"images\":[{\"id\":\"00000000-0000-0000-0000-000000000012\",\"position\":\"COVER\"}],\"ageRestriction\":0}";
            return Stream.of(
                Arguments.of(
                    "Значение пустое",
                    "",
                    requestBody,
                    new ErrorResponseDto(
                        400,
                        "Некорректный запрос",
                        "Проверка запроса не пройдена",
                        List.of(
                            new FieldErrorResponseDto(
                                "Idempotency-Key",
                                "Ключ идемпотентности не должен быть пустым")))),
                Arguments.of(
                    "Значение состоит из пробелов",
                    "   ",
                    requestBody,
                    new ErrorResponseDto(
                        400,
                        "Некорректный запрос",
                        "Проверка запроса не пройдена",
                        List.of(
                            new FieldErrorResponseDto(
                                "Idempotency-Key",
                                "Ключ идемпотентности не должен быть пустым")))));
    }

    public static Stream<Arguments> create_whenRequestIsRetried_thenCreateOnlyOneEventTestCases() {
            return Stream.of(
                Arguments.of(
                    "Идентичный запрос повторен с тем же ключом",
                    "{\"title\":\"Festival\",\"categoryIds\":[{\"id\":\"00000000-0000-0000-0000-000000000011\"}],\"images\":[{\"id\":\"00000000-0000-0000-0000-000000000014\",\"position\":\"GALLERY\"},{\"id\":\"00000000-0000-0000-0000-000000000012\",\"position\":\"COVER\"}],\"ageRestriction\":0}",
                    "create-retry"));
    }

    public static Stream<Arguments> create_whenImagesAreReused_thenKeepPositionsIndependentTestCases() {
            UUID coverId = UUID.fromString("00000000-0000-0000-0000-000000000012");
            UUID galleryId = UUID.fromString("00000000-0000-0000-0000-000000000014");
            return Stream.of(
                Arguments.of(
                    "Обложка одного события является галереей другого и наоборот",
                    "{\"title\":\"First event\",\"ageRestriction\":0,\"images\":[{\"id\":\"00000000-0000-0000-0000-000000000012\",\"position\":\"COVER\"},{\"id\":\"00000000-0000-0000-0000-000000000014\",\"position\":\"GALLERY\"}]}",
                    "{\"title\":\"Second event\",\"ageRestriction\":0,\"images\":[{\"id\":\"00000000-0000-0000-0000-000000000012\",\"position\":\"GALLERY\"},{\"id\":\"00000000-0000-0000-0000-000000000014\",\"position\":\"COVER\"}]}",
                    "reuse-first",
                    "reuse-second",
                    new CreateEventResponseDto(
                        null,
                        "organizer-1",
                        "First event",
                        null,
                        List.of(),
                        List.of(
                            new EventImageResponseDto(
                                coverId, ImagePosition.COVER, null),
                            new EventImageResponseDto(
                                galleryId, ImagePosition.GALLERY, null)),
                        0,
                        EventStatus.DRAFT,
                        null,
                        null,
                        null,
                        null,
                        null),
                    new CreateEventResponseDto(
                        null,
                        "organizer-1",
                        "Second event",
                        null,
                        List.of(),
                        List.of(
                            new EventImageResponseDto(
                                galleryId, ImagePosition.COVER, null),
                            new EventImageResponseDto(
                                coverId, ImagePosition.GALLERY, null)),
                        0,
                        EventStatus.DRAFT,
                        null,
                        null,
                        null,
                        null,
                        null),
                    List.of(
                        Map.of("image_id", coverId, "position", "COVER"),
                        Map.of("image_id", galleryId, "position", "GALLERY")),
                    List.of(
                        Map.of("image_id", galleryId, "position", "COVER"),
                        Map.of("image_id", coverId, "position", "GALLERY"))));
    }

    public static Stream<Arguments> create_whenOneHundredCategoriesAreRequested_thenPersistAllLinksTestCases() {
            String categoryIds = IntStream.rangeClosed(1, 100)
                .mapToObj("{\"id\":\"00000000-0000-0000-0000-%012d\"}"::formatted)
                .collect(Collectors.joining(","));
            List<UUID> expectedCategoryIds = IntStream.rangeClosed(1, 100)
                .mapToObj(index -> UUID.fromString("00000000-0000-0000-0000-%012d".formatted(index)))
                .toList();
            return Stream.of(
                Arguments.of(
                    "Сто активных категорий",
                    "{\"title\":\"Large event\",\"categoryIds\":["
                        + categoryIds
                        + "],\"images\":[{\"id\":\"00000000-0000-0000-0000-000000000012\",\"position\":\"COVER\"}],\"ageRestriction\":0}",
                    "create-hundred-categories",
                    expectedCategoryIds));
    }

    public static Stream<Arguments> create_whenIdempotencyKeyIsReusedForDifferentRequest_thenReturnConflictTestCases() {
            return Stream.of(
                Arguments.of(
                    "Тот же ключ использован для другого названия",
                    "{\"title\":\"Festival\",\"categoryIds\":[{\"id\":\"00000000-0000-0000-0000-000000000011\"}],\"images\":[{\"id\":\"00000000-0000-0000-0000-000000000012\",\"position\":\"COVER\"}],\"ageRestriction\":0}",
                    "{\"title\":\"Concert\",\"categoryIds\":[{\"id\":\"00000000-0000-0000-0000-000000000011\"}],\"images\":[{\"id\":\"00000000-0000-0000-0000-000000000012\",\"position\":\"COVER\"}],\"ageRestriction\":0}",
                    "create-conflict",
                    201,
                    "Festival",
                    null,
                    new ErrorResponseDto(
                        409,
                        "Конфликт ключа идемпотентности",
                        "Мероприятие с ключом идемпотентности create-conflict уже создано",
                        List.of())),
                Arguments.of(
                    "Разделитель перенесён из названия в описание",
                    "{\"title\":\"Festival|Live\",\"description\":\"Concert\",\"categoryIds\":[{\"id\":\"00000000-0000-0000-0000-000000000011\"}],\"images\":[{\"id\":\"00000000-0000-0000-0000-000000000012\",\"position\":\"COVER\"}],\"ageRestriction\":0}",
                    "{\"title\":\"Festival\",\"description\":\"Live|Concert\",\"categoryIds\":[{\"id\":\"00000000-0000-0000-0000-000000000011\"}],\"images\":[{\"id\":\"00000000-0000-0000-0000-000000000012\",\"position\":\"COVER\"}],\"ageRestriction\":0}",
                    "delimiter-conflict",
                    201,
                    "Festival|Live",
                    "Concert",
                    new ErrorResponseDto(
                        409,
                        "Конфликт ключа идемпотентности",
                        "Мероприятие с ключом идемпотентности delimiter-conflict уже создано",
                        List.of())),
                Arguments.of(
                    "Описание null заменено строкой null",
                    "{\"title\":\"Festival\",\"description\":null,\"categoryIds\":[{\"id\":\"00000000-0000-0000-0000-000000000011\"}],\"images\":[{\"id\":\"00000000-0000-0000-0000-000000000012\",\"position\":\"COVER\"}],\"ageRestriction\":0}",
                    "{\"title\":\"Festival\",\"description\":\"null\",\"categoryIds\":[{\"id\":\"00000000-0000-0000-0000-000000000011\"}],\"images\":[{\"id\":\"00000000-0000-0000-0000-000000000012\",\"position\":\"COVER\"}],\"ageRestriction\":0}",
                    "null-conflict",
                    201,
                    "Festival",
                    null,
                    new ErrorResponseDto(
                        409,
                        "Конфликт ключа идемпотентности",
                        "Мероприятие с ключом идемпотентности null-conflict уже создано",
                        List.of())));
    }
}
