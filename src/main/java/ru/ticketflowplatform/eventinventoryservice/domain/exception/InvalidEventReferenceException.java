package ru.ticketflowplatform.eventinventoryservice.domain.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

public class InvalidEventReferenceException extends ErrorResponseException {

    public InvalidEventReferenceException(
        String messageCode, String detailCode, Object... arguments) {
            super(
                HttpStatus.UNPROCESSABLE_CONTENT.value(),
                messageCode,
                detailCode,
                List.of(),
                arguments);
    }
}
