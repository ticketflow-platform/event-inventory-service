package ru.ticketflowplatform.eventinventoryservice.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.IdempotencyConflictException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.InvalidEventReferenceException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.InvalidEventRequestException;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;
import ru.ticketflowplatform.eventinventoryservice.mapper.ErrorMapperImpl;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(new ErrorMapperImpl());

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.GlobalExceptionHandlerTestCases#validation_whenRequestHasInvalidFields_shouldReturnBadRequestTestCases")
    @DisplayName("Обрабатывает ошибки валидации запроса по сценариям")
    void validation_whenRequestHasInvalidFields_shouldReturnBadRequestTest(
            String scenario,
            MethodArgumentNotValidException exception,
            ErrorResponseDto expected
    ) {
        Assertions.assertEquals(expected, handler.validation(exception));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.GlobalExceptionHandlerTestCases#missingHeader_whenRequiredHeaderIsAbsent_shouldReturnBadRequestTestCases")
    @DisplayName("Обрабатывает отсутствующий заголовок по сценариям")
    void missingHeader_whenRequiredHeaderIsAbsent_shouldReturnBadRequestTest(
            String scenario,
            MissingRequestHeaderException exception,
            ErrorResponseDto expected
    ) {
        Assertions.assertEquals(expected, handler.missingHeader(exception));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.GlobalExceptionHandlerTestCases#invalidReference_whenReferenceIsInvalid_shouldReturnUnprocessableEntityTestCases")
    @DisplayName("Обрабатывает недопустимую ссылку по сценариям")
    void invalidReference_whenReferenceIsInvalid_shouldReturnUnprocessableEntityTest(
            String scenario,
            InvalidEventReferenceException exception,
            ErrorResponseDto expected
    ) {
        Assertions.assertEquals(expected, handler.invalidReference(exception));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.GlobalExceptionHandlerTestCases#invalidRequest_whenRequestIsInvalid_shouldReturnBadRequestTestCases")
    @DisplayName("Обрабатывает недопустимый запрос по сценариям")
    void invalidRequest_whenRequestIsInvalid_shouldReturnBadRequestTest(
            String scenario,
            InvalidEventRequestException exception,
            ErrorResponseDto expected
    ) {
        Assertions.assertEquals(expected, handler.invalidRequest(exception));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.GlobalExceptionHandlerTestCases#idempotencyConflict_whenKeyHasDifferentRequest_shouldReturnConflictTestCases")
    @DisplayName("Обрабатывает конфликт ключа идемпотентности по сценариям")
    void idempotencyConflict_whenKeyHasDifferentRequest_shouldReturnConflictTest(
            String scenario,
            IdempotencyConflictException exception,
            ErrorResponseDto expected
    ) {
        Assertions.assertEquals(expected, handler.idempotencyConflict(exception));
    }
}
