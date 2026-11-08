package ru.ticketflowplatform.eventinventoryservice.domain.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

public class IdempotencyConflictException extends ErrorResponseException {

    public IdempotencyConflictException(String message, String detail) {
        super(HttpStatus.CONFLICT.value(), message, detail, List.of());
    }
}
