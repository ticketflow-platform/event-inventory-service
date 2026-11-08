package ru.ticketflowplatform.eventinventoryservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
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
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.CreateEventResponseDto;
import ru.ticketflowplatform.eventinventoryservice.service.EventService;

@RestController
@RequestMapping("${api.urls.v1.event}")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    @ResponseStatus(HttpStatus.CREATED)
    CreateEventResponseDto create(
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid CreateEventRequestDto request
    ) {
        return eventService.create(request, jwt.getSubject(), idempotencyKey);
    }
}
