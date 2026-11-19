package ru.ticketflowplatform.eventinventoryservice.domain.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

public class HashGenerationException extends ErrorResponseException {

    public HashGenerationException(Throwable cause) {
        super(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "api.error.hash_generation.message",
            "api.error.hash_generation.detail",
            cause,
            List.of()
        );
    }
}
