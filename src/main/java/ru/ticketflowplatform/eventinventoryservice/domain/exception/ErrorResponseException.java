package ru.ticketflowplatform.eventinventoryservice.domain.exception;

import java.util.List;
import lombok.Getter;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto.FieldErrorResponseDto;

@Getter
public class ErrorResponseException extends RuntimeException {

    private final int status;
    private final String message;
    private final String detail;
    private final List<FieldErrorResponseDto> errors;

    public ErrorResponseException(int status, String message, String detail, List<FieldErrorResponseDto> errors) {
        super(detail);
        this.status = status;
        this.message = message;
        this.detail = detail;
        this.errors = errors;
    }
}
