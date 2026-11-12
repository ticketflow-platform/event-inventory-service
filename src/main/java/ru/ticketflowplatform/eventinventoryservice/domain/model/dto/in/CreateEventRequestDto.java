package ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.ImageType;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record CreateEventRequestDto(
        @Schema(description = "Event title", requiredMode = REQUIRED, example = "Festival")
        @NotBlank
        String title,

        @Schema(description = "Event description", requiredMode = NOT_REQUIRED, example = "Live music event")
        String description,

        @Schema(
                description = "Active category references; required before publication",
                requiredMode = NOT_REQUIRED,
                example = "[{\"categoryId\":\"00000000-0000-0000-0000-000000000011\"}]"
        )
        List<@Valid CategoryRequestDto> categoryIds,

        @Schema(
                description = "Image references; required before publication",
                requiredMode = NOT_REQUIRED,
                example = "[{\"imageId\":\"00000000-0000-0000-0000-000000000012\",\"type\":\"COVER\"}]"
        )
        List<@Valid ImageRequestDto> images,

        @Schema(
                description = "Minimum attendee age; use 0 for no age restriction; required before publication",
                requiredMode = NOT_REQUIRED,
                example = "0",
                allowableValues = {"0", "6", "12", "16", "18"}
        )
        Integer ageRestriction
) {
    public record ImageRequestDto(
            @Schema(requiredMode = REQUIRED, example = "00000000-0000-0000-0000-000000000012")
            @NotNull UUID imageId,
            @Schema(requiredMode = REQUIRED, example = "COVER")
            @NotNull ImageType type
    ) {
    }

    public record CategoryRequestDto(
            @Schema(requiredMode = REQUIRED, example = "00000000-0000-0000-0000-000000000012")
            @NotNull UUID categoryId
    ) {
    }
}
