package scenario.cases.java.controller;

import helper.ExceptionHandlerTestHelper;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.EventNotFoundException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.HashGenerationException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.IdempotencyConflictException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.InvalidEventReferenceException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.InvalidEventRequestException;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto.FieldErrorResponseDto;

public final class GlobalExceptionHandlerTestCases {

    private GlobalExceptionHandlerTestCases() {
    }

    public static Stream<Arguments> resolveException_whenRequestFails_thenReturnLocalizedErrorTestCases() {
            Locale ru = Locale.forLanguageTag("ru");
            MethodParameter parameter = ExceptionHandlerTestHelper.eventParameter(2);
            BeanPropertyBindingResult russianErrors = new BeanPropertyBindingResult(
                new CreateEventRequestDto(" ", null, null, null, null), "request");
            russianErrors.addError(
                new FieldError("request", "title", "Название не должно быть пустым"));
            BeanPropertyBindingResult englishErrors = new BeanPropertyBindingResult(
                new CreateEventRequestDto(" ", null, null, null, null), "request");
            englishErrors.addError(new FieldError("request", "title", "Title must not be blank"));
            UUID eventId = new UUID(0, 1);
            return Stream.of(
                Arguments.of(
                    "Валидация тела: русский",
                    ExceptionHandlerTestHelper.request(ru),
                    new MethodArgumentNotValidException(parameter, russianErrors),
                    new ErrorResponseDto(
                        400,
                        "Некорректный запрос",
                        "Проверка запроса не пройдена",
                        List.of(
                            new FieldErrorResponseDto(
                                "title", "Название не должно быть пустым")))),
                Arguments.of(
                    "Валидация тела: английский",
                    ExceptionHandlerTestHelper.request(Locale.ENGLISH),
                    new MethodArgumentNotValidException(parameter, englishErrors),
                    new ErrorResponseDto(
                        400,
                        "Bad request",
                        "Request validation failed",
                        List.of(
                            new FieldErrorResponseDto(
                                "title", "Title must not be blank")))),
                Arguments.of(
                    "Отсутствует обязательный заголовок",
                    ExceptionHandlerTestHelper.request(Locale.ENGLISH),
                    new MissingRequestHeaderException(
                        "Idempotency-Key", ExceptionHandlerTestHelper.eventParameter(0)),
                    new ErrorResponseDto(
                        400,
                        "Bad request",
                        "Missing required header: Idempotency-Key",
                        List.of())),
                Arguments.of(
                    "Некорректное тело",
                    ExceptionHandlerTestHelper.request(ru),
                    new HttpMessageNotReadableException(
                        "Invalid JSON", new MockHttpInputMessage(new byte[0])),
                    new ErrorResponseDto(
                        400,
                        "Некорректный запрос",
                        "Тело запроса имеет некорректный формат",
                        List.of())),
                Arguments.of(
                    "Неизвестная ссылка",
                    ExceptionHandlerTestHelper.request(Locale.ENGLISH),
                    new InvalidEventReferenceException(
                        "api.event.categories.not_found.message.one",
                        "api.event.categories.not_found.detail.one",
                        eventId),
                    new ErrorResponseDto(
                        422,
                        "Category not found",
                        "Category with id " + eventId + " was not found",
                        List.of())),
                Arguments.of(
                    "Неподдерживаемый возраст",
                    ExceptionHandlerTestHelper.request(Locale.ENGLISH),
                    new InvalidEventRequestException(
                        "api.event.age_restriction.invalid.message",
                        "api.event.age_restriction.invalid.detail",
                        21),
                    new ErrorResponseDto(
                        400,
                        "Invalid age restriction",
                        "Supported values are 0, 6, 12, 16, and 18. Received value: 21",
                        List.of())),
                Arguments.of(
                    "Конфликт ключа",
                    ExceptionHandlerTestHelper.request(Locale.ENGLISH),
                    new IdempotencyConflictException(
                        "api.event.idempotency_conflict.message",
                        "api.event.idempotency_conflict.detail",
                        "key-1"),
                    new ErrorResponseDto(
                        409,
                        "Idempotency key conflict",
                        "An event with idempotency key key-1 has already been created",
                        List.of())),
                Arguments.of(
                    "Событие отсутствует",
                    ExceptionHandlerTestHelper.request(Locale.ENGLISH),
                    new EventNotFoundException(
                        "api.event.not_found.message",
                        "api.event.not_found.detail",
                        eventId,
                        "key-1"),
                    new ErrorResponseDto(
                        404,
                        "Event not found",
                        "Event with id " + eventId + " and key key-1 was not found",
                        List.of())),
                Arguments.of(
                    "Ошибка хеширования",
                    ExceptionHandlerTestHelper.request(Locale.ENGLISH),
                    new HashGenerationException(new NoSuchAlgorithmException("SHA-256")),
                    new ErrorResponseDto(
                        500,
                        "Request processing failed",
                        "Unable to generate the request fingerprint",
                        List.of())));
    }

    public static Stream<Arguments> resolveException_whenMethodValidationFails_thenIdentifyInvalidInputTestCases() {
            List<DefaultMessageSourceResolvable> errors = List.of(
                new DefaultMessageSourceResolvable(
                    new String[] { "validation.idempotency_key.not_blank" }));
            List<FieldErrorResponseDto> namedHeader = List.of(
                new FieldErrorResponseDto(
                    "Idempotency-Key", "Idempotency-Key must not be blank"));
            return Stream.of(
                Arguments.of(
                    "Явное имя заголовка",
                    ExceptionHandlerTestHelper.request(Locale.ENGLISH),
                    ExceptionHandlerTestHelper.methodValidation(
                        "Idempotency-Key", null, "key", errors),
                    new ErrorResponseDto(
                        400, "Bad request", "Request validation failed", namedHeader)),
                Arguments.of(
                    "Неявное имя заголовка",
                    ExceptionHandlerTestHelper.request(Locale.ENGLISH),
                    ExceptionHandlerTestHelper.methodValidation(
                        "", null, "idempotencyKey", errors),
                    new ErrorResponseDto(
                        400,
                        "Bad request",
                        "Request validation failed",
                        List.of(
                            new FieldErrorResponseDto(
                                "idempotencyKey",
                                "Idempotency-Key must not be blank")))),
                Arguments.of(
                    "Явное имя query-параметра",
                    ExceptionHandlerTestHelper.request(Locale.ENGLISH),
                    ExceptionHandlerTestHelper.methodValidation(
                        null, "key", "idempotencyKey", errors),
                    new ErrorResponseDto(
                        400,
                        "Bad request",
                        "Request validation failed",
                        List.of(
                            new FieldErrorResponseDto(
                                "key", "Idempotency-Key must not be blank")))),
                Arguments.of(
                    "Неявное имя query-параметра",
                    ExceptionHandlerTestHelper.request(Locale.ENGLISH),
                    ExceptionHandlerTestHelper.methodValidation(
                        null, "", "idempotencyKey", errors),
                    new ErrorResponseDto(
                        400,
                        "Bad request",
                        "Request validation failed",
                        List.of(
                            new FieldErrorResponseDto(
                                "idempotencyKey",
                                "Idempotency-Key must not be blank")))),
                Arguments.of(
                    "Параметр без аннотаций",
                    ExceptionHandlerTestHelper.request(Locale.ENGLISH),
                    ExceptionHandlerTestHelper.methodValidation(null, null, "key", errors),
                    new ErrorResponseDto(
                        400,
                        "Bad request",
                        "Request validation failed",
                        List.of(
                            new FieldErrorResponseDto(
                                "key", "Idempotency-Key must not be blank")))),
                Arguments.of(
                    "Имя параметра недоступно",
                    ExceptionHandlerTestHelper.request(Locale.ENGLISH),
                    ExceptionHandlerTestHelper.methodValidation(null, null, null, errors),
                    new ErrorResponseDto(
                        400,
                        "Bad request",
                        "Request validation failed",
                        List.of(
                            new FieldErrorResponseDto(
                                "request", "Idempotency-Key must not be blank")))),
                Arguments.of(
                    "Ошибка вложенного поля тела",
                    ExceptionHandlerTestHelper.request(Locale.ENGLISH),
                    ExceptionHandlerTestHelper.methodValidation(
                        null,
                        null,
                        "request",
                        List.of(
                            new FieldError(
                                "request",
                                "title",
                                null,
                                false,
                                new String[] { "validation.event.title.not_blank" },
                                null,
                                null))),
                    new ErrorResponseDto(
                        400,
                        "Bad request",
                        "Request validation failed",
                        List.of(
                            new FieldErrorResponseDto(
                                "title", "Title must not be blank")))));
    }
}
