package ru.ticketflowplatform.eventinventoryservice.domain.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

public class EventNotFoundException extends ErrorResponseException {
    public EventNotFoundException(String message, String detail) {
        super(HttpStatus.NOT_FOUND.value(), message, detail, List.of());
    }
}
