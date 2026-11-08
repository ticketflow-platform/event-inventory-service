package ru.ticketflowplatform.eventinventoryservice.mapper;

import java.time.Instant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Event;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.IdempotencyKey;

class IdempotencyKeyMapperTest {

    private final IdempotencyKeyMapper idempotencyKeyMapper = new IdempotencyKeyMapperImpl();

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.IdempotencyKeyMapperTestCases#toEntity_whenIdempotencyDataIsProvided_shouldMapKeyTestCases")
    @DisplayName("Преобразует данные идемпотентности в сущность по сценариям")
    void toEntity_whenIdempotencyDataIsProvided_shouldMapKeyTest(
            String scenario,
            String organizerId,
            String key,
            String requestHash,
            Event event,
            Instant now
    ) {
        IdempotencyKey idempotencyKey = idempotencyKeyMapper.toEntity(organizerId, key, requestHash, event, now);

        Assertions.assertNull(idempotencyKey.getId());
        Assertions.assertEquals(organizerId, idempotencyKey.getOrganizerId());
        Assertions.assertEquals(key, idempotencyKey.getKey());
        Assertions.assertEquals(requestHash, idempotencyKey.getRequestHash());
        Assertions.assertSame(event, idempotencyKey.getEvent());
        Assertions.assertEquals(now, idempotencyKey.getCreatedAt());
    }
}
