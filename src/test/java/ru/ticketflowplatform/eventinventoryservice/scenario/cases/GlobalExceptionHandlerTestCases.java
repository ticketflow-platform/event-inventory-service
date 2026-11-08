package ru.ticketflowplatform.eventinventoryservice.scenario.cases;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.http.HttpStatus;
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
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
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
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        "Missing required header: Idempotency-Key",
                        List.of()
                )
        ));
    }

    public static Stream<Arguments> invalidReference_whenReferenceIsInvalid_shouldReturnUnprocessableEntityTestCases() {
        return Stream.of(
                Arguments.of(
                        "Категория отсутствует",
                        new InvalidEventReferenceException(HttpStatus.UNPROCESSABLE_CONTENT.getReasonPhrase(), "Category not found"),
                        new ErrorResponseDto(422, HttpStatus.UNPROCESSABLE_CONTENT.getReasonPhrase(), "Category not found", List.of())
                ),
                Arguments.of(
                        "Изображение принадлежит другому организатору",
                        new InvalidEventReferenceException(HttpStatus.UNPROCESSABLE_CONTENT.getReasonPhrase(), "Image is not owned by organizer"),
                        new ErrorResponseDto(422, HttpStatus.UNPROCESSABLE_CONTENT.getReasonPhrase(), "Image is not owned by organizer", List.of())
                )
        );
    }

    public static Stream<Arguments> invalidRequest_whenRequestIsInvalid_shouldReturnBadRequestTestCases() {
        return Stream.of(
                Arguments.of(
                        "Пустой ключ",
                        new InvalidEventRequestException(HttpStatus.BAD_REQUEST.getReasonPhrase(), "Idempotency-Key must not be blank"),
                        new ErrorResponseDto(400, HttpStatus.BAD_REQUEST.getReasonPhrase(), "Idempotency-Key must not be blank", List.of())
                ),
                Arguments.of(
                        "Возрастное ограничение не поддерживается",
                        new InvalidEventRequestException(HttpStatus.BAD_REQUEST.getReasonPhrase(), "Unsupported age restriction"),
                        new ErrorResponseDto(400, HttpStatus.BAD_REQUEST.getReasonPhrase(), "Unsupported age restriction", List.of())
                )
        );
    }

    public static Stream<Arguments> idempotencyConflict_whenKeyHasDifferentRequest_shouldReturnConflictTestCases() {
        return Stream.of(Arguments.of(
                "Повторное использование ключа с другим запросом",
                new IdempotencyConflictException(HttpStatus.CONFLICT.getReasonPhrase(), "Idempotency-Key conflict"),
                new ErrorResponseDto(409, HttpStatus.CONFLICT.getReasonPhrase(), "Idempotency-Key conflict", List.of())
        ));
    }
}
