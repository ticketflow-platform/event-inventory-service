package ru.ticketflowplatform.eventinventoryservice.controller;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.EventNotFoundException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.HashGenerationException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.IdempotencyConflictException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.InvalidEventReferenceException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.InvalidEventRequestException;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto.FieldErrorResponseDto;
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
            messageSource.getMessage("api.error.bad_request.message", null, locale),
            messageSource.getMessage("api.error.validation.detail", null, locale),
            exception.getBindingResult().getFieldErrors().stream()
                .map(errorMapper::toFieldError)
                .toList());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponseDto methodValidation(HandlerMethodValidationException exception, Locale locale) {
        return errorMapper.toResponse(
            HttpStatus.BAD_REQUEST.value(),
            messageSource.getMessage("api.error.bad_request.message", null, locale),
            messageSource.getMessage("api.error.validation.detail", null, locale),
            prepareErrors(exception, locale));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponseDto missingHeader(MissingRequestHeaderException exception, Locale locale) {
        return errorMapper.toResponse(
            HttpStatus.BAD_REQUEST.value(),
            messageSource.getMessage("api.error.bad_request.message", null, locale),
            messageSource.getMessage(
                "api.error.header_missing.detail",
                new Object[] { exception.getHeaderName() },
                locale),
            List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponseDto unreadableRequest(Locale locale) {
        return errorMapper.toResponse(
            HttpStatus.BAD_REQUEST.value(),
            messageSource.getMessage("api.error.bad_request.message", null, locale),
            messageSource.getMessage("api.error.body_malformed.detail", null, locale),
            List.of());
    }

    @ExceptionHandler(InvalidEventReferenceException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    ErrorResponseDto invalidReference(InvalidEventReferenceException exception, Locale locale) {
        return errorMapper.toResponse(
            exception.getStatus(),
            messageSource.getMessage(
                exception.getMessageCode(),
                exception.getArguments(),
                locale),
            messageSource.getMessage(
                exception.getDetailCode(),
                exception.getArguments(),
                locale),
            exception.getErrors());
    }

    @ExceptionHandler(InvalidEventRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponseDto invalidRequest(InvalidEventRequestException exception, Locale locale) {
        return errorMapper.toResponse(
            exception.getStatus(),
            messageSource.getMessage(
                exception.getMessageCode(),
                exception.getArguments(),
                locale),
            messageSource.getMessage(
                exception.getDetailCode(),
                exception.getArguments(),
                locale),
            exception.getErrors());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ErrorResponseDto idempotencyConflict(IdempotencyConflictException exception, Locale locale) {
        return errorMapper.toResponse(
            exception.getStatus(),
            messageSource.getMessage(
                exception.getMessageCode(),
                exception.getArguments(),
                locale),
            messageSource.getMessage(
                exception.getDetailCode(),
                exception.getArguments(),
                locale),
            exception.getErrors());
    }

    @ExceptionHandler(EventNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorResponseDto eventNotFound(EventNotFoundException exception, Locale locale) {
        return errorMapper.toResponse(
            exception.getStatus(),
            messageSource.getMessage(
                exception.getMessageCode(),
                exception.getArguments(),
                locale),
            messageSource.getMessage(
                exception.getDetailCode(),
                exception.getArguments(),
                locale),
            exception.getErrors());
    }

    @ExceptionHandler(HashGenerationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ErrorResponseDto hashGenerationFailure(HashGenerationException exception, Locale locale) {
        return errorMapper.toResponse(
            exception.getStatus(),
            messageSource.getMessage(
                exception.getMessageCode(),
                exception.getArguments(),
                locale),
            messageSource.getMessage(
                exception.getDetailCode(),
                exception.getArguments(),
                locale),
            exception.getErrors());
    }

    private List<FieldErrorResponseDto> prepareErrors(
        HandlerMethodValidationException exception,
        Locale locale) {
        return exception.getParameterValidationResults().stream()
            .flatMap(toFieldError(locale))
            .toList();
    }

    private Function<? super ParameterValidationResult, ? extends Stream<? extends FieldErrorResponseDto>> toFieldError(
        Locale locale) {
        return result -> result.getResolvableErrors().stream()
            .map(
                error -> new ErrorResponseDto.FieldErrorResponseDto(
                    error instanceof FieldError fieldError
                        ? fieldError.getField()
                        : parameterName(result),
                    messageSource.getMessage(error, locale)));
    }

    private String parameterName(ParameterValidationResult result) {
        RequestHeader header = result.getMethodParameter().getParameterAnnotation(RequestHeader.class);
        if (header != null && !header.name().isBlank()) {
            return header.name();
        }
        RequestParam requestParam = result.getMethodParameter().getParameterAnnotation(RequestParam.class);
        if (requestParam != null && !requestParam.name().isBlank()) {
            return requestParam.name();
        }

        return Objects.toString(result.getMethodParameter().getParameterName(), "request");
    }
}
