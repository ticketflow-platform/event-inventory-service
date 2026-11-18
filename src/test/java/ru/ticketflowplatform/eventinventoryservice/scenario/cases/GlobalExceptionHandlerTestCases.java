package ru.ticketflowplatform.eventinventoryservice.scenario.cases;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.IdempotencyConflictException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.InvalidEventReferenceException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.InvalidEventRequestException;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;

public final class GlobalExceptionHandlerTestCases {

    public static Stream<Arguments> validation_whenRequestHasInvalidFields_shouldReturnBadRequestTestCases() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new org.springframework.validation.FieldError(
                "request", "title", null, false, null, null, "must not be blank"
        ));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);
        return Stream.of(Arguments.of(
                "Пустое название",
                exception,
                new ErrorResponseDto(
                        400,
                        "Bad request",
                        "Request validation failed",
                        List.of(new ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto.FieldErrorResponseDto("title", "must not be blank"))
                )
        ));
    }

    public static Stream<Arguments> missingHeader_whenRequiredHeaderIsAbsent_shouldReturnBadRequestTestCases() {
        return Stream.of(Arguments.of(
                "Нет Idempotency-Key",
                new MissingRequestHeaderException("Idempotency-Key", null),
                new ErrorResponseDto(
                        400,
                        "Bad request",
                        "Missing required header: Idempotency-Key",
                        List.of()
                )
        ));
    }

    public static Stream<Arguments> invalidReference_whenReferenceIsInvalid_shouldReturnUnprocessableEntityTestCases() {
        return Stream.of(
                Arguments.of(
                        "Категория отсутствует",
                        new InvalidEventReferenceException(
                                "api.event.categories.not_found.message.one",
                                "api.event.categories.not_found.detail.one",
                                "category-1"
                        ),
                        new ErrorResponseDto(422, "Category not found", "Category with id category-1 was not found", List.of())
                ),
                Arguments.of(
                        "Изображение принадлежит другому организатору",
                        new InvalidEventReferenceException(
                                "api.event.images.not_found.message.one",
                                "api.event.images.not_found.detail.one",
                                "image-1"
                        ),
                        new ErrorResponseDto(422, "Image not found", "Image with id image-1 was not found", List.of())
                )
        );
    }

    public static Stream<Arguments> invalidRequest_whenRequestIsInvalid_shouldReturnBadRequestTestCases() {
        return Stream.of(
                Arguments.of(
                        "Пустой ключ",
                        new InvalidEventRequestException(
                                "api.error.bad_request.message",
                                "api.error.header_missing.detail",
                                "Idempotency-Key"
                        ),
                        new ErrorResponseDto(400, "Bad request", "Missing required header: Idempotency-Key", List.of())
                ),
                Arguments.of(
                        "Возрастное ограничение не поддерживается",
                        new InvalidEventRequestException(
                                "api.event.age_restriction.invalid.message",
                                "api.event.age_restriction.invalid.detail"
                        ),
                        new ErrorResponseDto(400, "Invalid age restriction", "Supported values are 0, 6, 12, 16, and 18", List.of())
                )
        );
    }

    public static Stream<Arguments> idempotencyConflict_whenKeyHasDifferentRequest_shouldReturnConflictTestCases() {
        return Stream.of(Arguments.of(
                "Повторное использование ключа с другим запросом",
                new IdempotencyConflictException(
                        "api.event.idempotency_conflict.message",
                        "api.event.idempotency_conflict.detail",
                        "key-1"
                ),
                new ErrorResponseDto(409, "Idempotency key conflict", "An event with idempotency key key-1 has already been created", List.of())
        ));
    }
}
