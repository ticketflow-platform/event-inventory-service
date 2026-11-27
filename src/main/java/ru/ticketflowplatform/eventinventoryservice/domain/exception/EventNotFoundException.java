package ru.ticketflowplatform.eventinventoryservice.domain.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

public class EventNotFoundException extends ErrorResponseException {
    public EventNotFoundException(String messageCode, String detailCode, Object... arguments) {
            super(HttpStatus.NOT_FOUND.value(), messageCode, detailCode, List.of(), arguments);
    }
}
