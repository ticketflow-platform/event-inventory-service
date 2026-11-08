package ru.ticketflowplatform.eventinventoryservice.domain.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

public class InvalidEventRequestException extends ErrorResponseException {

    public InvalidEventRequestException(String message, String detail) {
        super(HttpStatus.BAD_REQUEST.value(), message, detail, List.of());
    }
}
