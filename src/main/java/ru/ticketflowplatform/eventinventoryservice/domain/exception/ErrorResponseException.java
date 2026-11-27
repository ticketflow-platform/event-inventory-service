package ru.ticketflowplatform.eventinventoryservice.domain.exception;

import java.util.List;
import lombok.Getter;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto.FieldErrorResponseDto;

@Getter
public class ErrorResponseException extends RuntimeException {

    private final int status;
    private final String messageCode;
    private final String detailCode;
    private final Object[] arguments;
    private final List<FieldErrorResponseDto> errors;

    public ErrorResponseException(
        int status,
        String messageCode,
        String detailCode,
        List<FieldErrorResponseDto> errors,
        Object... arguments) {
            this(status, messageCode, detailCode, null, errors, arguments);
    }

    protected ErrorResponseException(
        int status,
        String messageCode,
        String detailCode,
        Throwable cause,
        List<FieldErrorResponseDto> errors,
        Object... arguments) {
            super(detailCode, cause);
            this.status = status;
            this.messageCode = messageCode;
            this.detailCode = detailCode;
            this.arguments = arguments.clone();
            this.errors = errors;
    }
}
