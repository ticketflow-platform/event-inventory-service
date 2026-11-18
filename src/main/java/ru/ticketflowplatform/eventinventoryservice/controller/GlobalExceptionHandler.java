package ru.ticketflowplatform.eventinventoryservice.controller;

import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.ErrorResponseException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.EventNotFoundException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.HashGenerationException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.IdempotencyConflictException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.InvalidEventReferenceException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.InvalidEventRequestException;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;
import ru.ticketflowplatform.eventinventoryservice.mapper.ErrorMapper;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ErrorMapper errorMapper;
    private final MessageSource messageSource;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponseDto validation(MethodArgumentNotValidException exception, Locale locale) {
        return errorMapper.toResponse(
            HttpStatus.BAD_REQUEST.value(),
            message("api.error.bad_request.message", locale),
            message("api.error.validation.detail", locale),
            exception.getBindingResult().getFieldErrors().stream().map(errorMapper::toFieldError).toList()
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponseDto methodValidation(HandlerMethodValidationException exception, Locale locale) {
        List<ErrorResponseDto.FieldErrorResponseDto> errors = exception.getParameterValidationResults().stream().flatMap(
            result -> result.getResolvableErrors().stream().map(
                error -> new ErrorResponseDto.FieldErrorResponseDto(
                        error instanceof FieldError fieldError ? fieldError.getField() : parameterName(result),
                        messageSource.getMessage(error, locale)
                )
            )
        ).toList();

        return errorMapper.toResponse(
            HttpStatus.BAD_REQUEST.value(),
            message("api.error.bad_request.message", locale),
            message("api.error.validation.detail", locale),
            errors
        );
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponseDto missingHeader(MissingRequestHeaderException exception, Locale locale) {
        return errorMapper.toResponse(
            HttpStatus.BAD_REQUEST.value(),
            message("api.error.bad_request.message", locale),
            message("api.error.header_missing.detail", locale, exception.getHeaderName()),
            List.of()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponseDto unreadableRequest(Locale locale) {
        return errorMapper.toResponse(
            HttpStatus.BAD_REQUEST.value(),
            message("api.error.bad_request.message", locale),
            message("api.error.body_malformed.detail", locale),
            List.of()
        );
    }

    @ExceptionHandler(InvalidEventReferenceException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    ErrorResponseDto invalidReference(InvalidEventReferenceException exception, Locale locale) {
        return localized(exception, locale);
    }

    @ExceptionHandler(InvalidEventRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponseDto invalidRequest(InvalidEventRequestException exception, Locale locale) {
        return localized(exception, locale);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ErrorResponseDto idempotencyConflict(IdempotencyConflictException exception, Locale locale) {
        return localized(exception, locale);
    }

    @ExceptionHandler(EventNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorResponseDto eventNotFound(EventNotFoundException exception, Locale locale) {
        return localized(exception, locale);
    }

    @ExceptionHandler(HashGenerationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ErrorResponseDto hashGenerationFailure(HashGenerationException exception, Locale locale) {
        return localized(exception, locale);
    }

    private ErrorResponseDto localized(ErrorResponseException exception, Locale locale) {
        return errorMapper.toResponse(
            exception.getStatus(),
            message(exception.getMessageCode(), locale, exception.getArguments()),
            message(exception.getDetailCode(), locale, exception.getArguments()),
            exception.getErrors()
        );
    }

    private String message(String code, Locale locale, Object... arguments) {
        return messageSource.getMessage(code, arguments, locale);
    }

    private String parameterName(ParameterValidationResult result) {
        RequestHeader header = result.getMethodParameter().getParameterAnnotation(RequestHeader.class);
        if (header != null) {
            if (!header.name().isBlank()) {
                return header.name();
            }
            if (!header.value().isBlank()) {
                return header.value();
            }
        }

        String name = result.getMethodParameter().getParameterName();
        return name == null ? "request" : name;
    }
}
