package ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.ImageType;

public record CreateEventRequestDto(
        @Schema(
                description = "i18n:swagger.event.create.title",
                requiredMode = REQUIRED,
                example = "i18n:swagger.event.create.title.example") @NotBlank(message = "{validation.event.title.not_blank}") String title,

        @Schema(
                description = "i18n:swagger.event.create.description",
                requiredMode = NOT_REQUIRED,
                example = "i18n:swagger.event.create.description.example") String description,

        @Schema(
                description = "i18n:swagger.event.create.category_ids",
                requiredMode = NOT_REQUIRED,
                example = "[{\"id\":\"00000000-0000-0000-0000-000000000011\"}]") List<@Valid CategoryRequestDto> categoryIds,

        @Schema(
                description = "i18n:swagger.event.create.images",
                requiredMode = NOT_REQUIRED,
                example = "[{\"id\":\"00000000-0000-0000-0000-000000000012\",\"type\":\"COVER\"}]") List<@Valid ImageRequestDto> images,

        @Schema(
                description = "i18n:swagger.event.create.age_restriction",
                requiredMode = NOT_REQUIRED,
                example = "0",
                allowableValues = { "0", "6", "12", "16", "18" }) Integer ageRestriction) {
    public record ImageRequestDto(
            @Schema(
                    description = "i18n:swagger.event.create.image.id",
                    requiredMode = REQUIRED,
                    example = "00000000-0000-0000-0000-000000000012") @NotNull(message = "{validation.event.image.id.not_null}") UUID id,

            @Schema(description = "i18n:swagger.event.create.image.type", requiredMode = REQUIRED, example = "COVER") @NotNull(
                    message = "{validation.event.image.type.not_null}") ImageType type) {
    }

    public record CategoryRequestDto(@Schema(
            description = "i18n:swagger.event.create.category.id",
            requiredMode = REQUIRED,
            example = "00000000-0000-0000-0000-000000000012") @NotNull(message = "{validation.event.category.id.not_null}") UUID id) {
    }
}
