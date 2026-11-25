package scenario.cases.java.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.EventNotFoundException;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;

public final class EventNotFoundApiTestCases {

    public static Stream<Arguments> create_whenStoredEventIsMissing_thenReturnNotFoundTestCases() {
        UUID eventId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        String idempotencyKey = "missing-event";
        return Stream.of(
            Arguments.of(
                "Ключ ссылается на удаленное событие",
                "{\"title\":\"Festival\"}",
                idempotencyKey,
                new EventNotFoundException(
                    "api.event.not_found.message",
                    "api.event.not_found.detail",
                    eventId,
                    idempotencyKey
                ),
                new ErrorResponseDto(
                    404,
                    "Event not found",
                    "Event with id " + eventId + " and key " + idempotencyKey + " was not found",
                    List.of()
                )
            )
        );
    }

    private EventNotFoundApiTestCases() {
    }
}
