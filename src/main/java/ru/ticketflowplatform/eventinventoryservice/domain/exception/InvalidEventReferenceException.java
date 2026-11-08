package ru.ticketflowplatform.eventinventoryservice.domain.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

public class InvalidEventReferenceException extends ErrorResponseException {

    public InvalidEventReferenceException(String message, String detail) {
        super(HttpStatus.UNPROCESSABLE_CONTENT.value(), message, detail, List.of());
    }
}
