package ru.ticketflowplatform.eventinventoryservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.CreateEventResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;
import ru.ticketflowplatform.eventinventoryservice.service.EventService;

@RestController
@RequestMapping("${api.urls.v1.event}")
@RequiredArgsConstructor
@Tag(name = "i18n:swagger.events.tag.name", description = "i18n:swagger.events.tag.description")
public class EventController {

    private final EventService eventService;

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "i18n:swagger.events.create.summary", description = "i18n:swagger.events.create.description")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "i18n:swagger.events.create.response.201"),
            @ApiResponse(
                    responseCode = "400",
                    description = "i18n:swagger.events.create.response.400",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "i18n:swagger.events.create.response.401",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "i18n:swagger.events.create.response.403",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "i18n:swagger.events.create.response.404",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "i18n:swagger.events.create.response.409",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(
                    responseCode = "422",
                    description = "i18n:swagger.events.create.response.422",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(
                    responseCode = "500",
                    description = "i18n:swagger.events.create.response.500",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))) })
    CreateEventResponseDto create(
            @Parameter(description = "i18n:swagger.events.create.idempotency_key", required = true)
            @RequestHeader("Idempotency-Key")
            @NotBlank(message = "{validation.idempotency_key.not_blank}") String idempotencyKey,

            @AuthenticationPrincipal Jwt jwt,

            @RequestBody
            @Valid CreateEventRequestDto request) {
        return eventService.create(request, jwt.getSubject(), idempotencyKey);
    }
}
