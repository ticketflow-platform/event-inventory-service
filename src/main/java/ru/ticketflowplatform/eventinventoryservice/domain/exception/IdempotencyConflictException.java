package ru.ticketflowplatform.eventinventoryservice.domain.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

public class IdempotencyConflictException extends ErrorResponseException {

    public IdempotencyConflictException(
        String messageCode, String detailCode, Object... arguments) {
        super(HttpStatus.CONFLICT.value(), messageCode, detailCode, List.of(), arguments);
    }
}
