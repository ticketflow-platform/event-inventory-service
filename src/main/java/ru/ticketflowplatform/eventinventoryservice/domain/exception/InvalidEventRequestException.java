package ru.ticketflowplatform.eventinventoryservice.domain.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

public class InvalidEventRequestException extends ErrorResponseException {

    public InvalidEventRequestException(String messageCode, String detailCode, Object... arguments) {
        super(HttpStatus.BAD_REQUEST.value(), messageCode, detailCode, List.of(), arguments);
    }
}
