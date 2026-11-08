package ru.ticketflowplatform.eventinventoryservice.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.IdempotencyConflictException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.InvalidEventReferenceException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.InvalidEventRequestException;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;
import ru.ticketflowplatform.eventinventoryservice.mapper.ErrorMapper;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ErrorMapper errorMapper;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponseDto validation(MethodArgumentNotValidException exception) {
        return errorMapper.toResponse(400, "Bad Request", "Request validation failed",
                exception.getBindingResult().getFieldErrors().stream().map(errorMapper::toFieldError).toList());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponseDto methodValidation(HandlerMethodValidationException exception) {
        List<FieldError> fieldErrors = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .filter(FieldError.class::isInstance)
                .map(FieldError.class::cast)
                .toList();

        return errorMapper.toResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request",
                "Request validation failed", fieldErrors.stream().map(errorMapper::toFieldError).toList());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponseDto missingHeader(MissingRequestHeaderException exception) {
        return errorMapper.toResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request",
                String.format("Missing required header: %s", exception.getHeaderName()), List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponseDto unreadableRequest(HttpMessageNotReadableException exception) {
        return errorMapper.toResponse(HttpStatus.BAD_REQUEST.value(),
                "Bad Request", "Malformed request body", List.of());
    }

    @ExceptionHandler(InvalidEventReferenceException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    ErrorResponseDto invalidReference(InvalidEventReferenceException exception) {
        return errorMapper.toResponse(exception.getStatus(), exception.getMessage(), exception.getDetail(), exception.getErrors());
    }

    @ExceptionHandler(InvalidEventRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponseDto invalidRequest(InvalidEventRequestException exception) {
        return errorMapper.toResponse(exception.getStatus(), exception.getMessage(), exception.getDetail(), exception.getErrors());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ErrorResponseDto idempotencyConflict(IdempotencyConflictException exception) {
        return errorMapper.toResponse(exception.getStatus(), exception.getMessage(), exception.getDetail(), exception.getErrors());
    }
}
