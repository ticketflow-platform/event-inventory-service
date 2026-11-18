package ru.ticketflowplatform.eventinventoryservice.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Locale;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.IdempotencyConflictException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.HashGenerationException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.InvalidEventReferenceException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.InvalidEventRequestException;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;
import ru.ticketflowplatform.eventinventoryservice.mapper.ErrorMapperImpl;

class GlobalExceptionHandlerTest {

    private final MessageSource messageSource = messageSource();
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(new ErrorMapperImpl(), messageSource);

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.GlobalExceptionHandlerTestCases#validation_whenRequestHasInvalidFields_shouldReturnBadRequestTestCases")
    @DisplayName("Обрабатывает ошибки валидации запроса по сценариям")
    void validation_whenRequestHasInvalidFields_shouldReturnBadRequestTest(
            String scenario,
            MethodArgumentNotValidException exception,
            ErrorResponseDto expected
    ) {
        Assertions.assertEquals(expected, handler.validation(exception, Locale.ENGLISH));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.GlobalExceptionHandlerTestCases#missingHeader_whenRequiredHeaderIsAbsent_shouldReturnBadRequestTestCases")
    @DisplayName("Обрабатывает отсутствующий заголовок по сценариям")
    void missingHeader_whenRequiredHeaderIsAbsent_shouldReturnBadRequestTest(
            String scenario,
            MissingRequestHeaderException exception,
            ErrorResponseDto expected
    ) {
        Assertions.assertEquals(expected, handler.missingHeader(exception, Locale.ENGLISH));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.GlobalExceptionHandlerTestCases#invalidReference_whenReferenceIsInvalid_shouldReturnUnprocessableEntityTestCases")
    @DisplayName("Обрабатывает недопустимую ссылку по сценариям")
    void invalidReference_whenReferenceIsInvalid_shouldReturnUnprocessableEntityTest(
            String scenario,
            InvalidEventReferenceException exception,
            ErrorResponseDto expected
    ) {
        Assertions.assertEquals(expected, handler.invalidReference(exception, Locale.ENGLISH));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.GlobalExceptionHandlerTestCases#invalidRequest_whenRequestIsInvalid_shouldReturnBadRequestTestCases")
    @DisplayName("Обрабатывает недопустимый запрос по сценариям")
    void invalidRequest_whenRequestIsInvalid_shouldReturnBadRequestTest(
            String scenario,
            InvalidEventRequestException exception,
            ErrorResponseDto expected
    ) {
        Assertions.assertEquals(expected, handler.invalidRequest(exception, Locale.ENGLISH));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.GlobalExceptionHandlerTestCases#idempotencyConflict_whenKeyHasDifferentRequest_shouldReturnConflictTestCases")
    @DisplayName("Обрабатывает конфликт ключа идемпотентности по сценариям")
    void idempotencyConflict_whenKeyHasDifferentRequest_shouldReturnConflictTest(
            String scenario,
            IdempotencyConflictException exception,
            ErrorResponseDto expected
    ) {
        Assertions.assertEquals(expected, handler.idempotencyConflict(exception, Locale.ENGLISH));
    }

    @Test
    @DisplayName("Локализует доменные ошибки на русском и английском")
    void invalidRequest_shouldUseRequestedLocale() {
        InvalidEventRequestException exception = new InvalidEventRequestException(
                "api.event.age_restriction.invalid.message",
                "api.event.age_restriction.invalid.detail"
        );

        Assertions.assertEquals(
                "Недопустимое возрастное ограничение",
                handler.invalidRequest(exception, Locale.forLanguageTag("ru")).message()
        );
        Assertions.assertEquals(
                "Invalid age restriction",
                handler.invalidRequest(exception, Locale.ENGLISH).message()
        );
    }

    @Test
    @DisplayName("Локализует ошибку формирования хеша и сохраняет причину")
    void hashGenerationFailure_shouldUseRequestedLocaleAndPreserveCause() {
        NoSuchAlgorithmException cause = new NoSuchAlgorithmException("SHA-256");
        HashGenerationException exception = new HashGenerationException(cause);

        ErrorResponseDto russian = handler.hashGenerationFailure(exception, Locale.forLanguageTag("ru"));
        ErrorResponseDto english = handler.hashGenerationFailure(exception, Locale.ENGLISH);

        Assertions.assertEquals(500, russian.status());
        Assertions.assertEquals("Не удалось сформировать контрольную сумму запроса", russian.detail());
        Assertions.assertEquals("Unable to generate the request fingerprint", english.detail());
        Assertions.assertSame(cause, exception.getCause());
    }

    private static MessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/messages");
        source.setDefaultEncoding("UTF-8");
        return source;
    }
}
