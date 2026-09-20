package ru.ticketflowplatform.eventinventoryservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.EventRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.CreateEventResponseDto;
import ru.ticketflowplatform.eventinventoryservice.service.EventService;

@RestController("${api.urls.v1.event}")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    CreateEventResponseDto create(@RequestBody @Valid EventRequestDto request) {

    }
}
