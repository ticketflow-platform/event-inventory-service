package ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out;

import java.util.List;

public record ErrorResponseDto(int status, String message, String detail, List<FieldErrorResponseDto> errors) {
    public record FieldErrorResponseDto(String field, String message) {
    }
}
