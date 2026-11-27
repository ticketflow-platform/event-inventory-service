package ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "i18n:swagger.error.response.schema")
public record ErrorResponseDto(
    @Schema(description = "i18n:swagger.error.response.status") int status,
    @Schema(description = "i18n:swagger.error.response.message") String message,
    @Schema(description = "i18n:swagger.error.response.detail") String detail,
    @Schema(description = "i18n:swagger.error.response.errors") List<FieldErrorResponseDto> errors) {

    @Schema(description = "i18n:swagger.error.response.field_error.schema")
    public record FieldErrorResponseDto(
        @Schema(description = "i18n:swagger.error.response.field") String field,
        @Schema(description = "i18n:swagger.error.response.field_message") String message) {
    }
}
