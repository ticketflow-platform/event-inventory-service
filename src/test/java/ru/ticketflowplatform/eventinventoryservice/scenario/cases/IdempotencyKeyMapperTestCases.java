package ru.ticketflowplatform.eventinventoryservice.scenario.cases;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Event;

public final class IdempotencyKeyMapperTestCases {

    public static Stream<Arguments> toEntity_whenIdempotencyDataIsProvided_shouldMapKeyTestCases() {
        Event event = new Event();
        event.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        return Stream.of(Arguments.of(
                "Ключ организатора",
                "organizer-1",
                "request-1",
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                event,
                Instant.parse("2026-01-02T03:04:05Z")
        ));
    }
}
