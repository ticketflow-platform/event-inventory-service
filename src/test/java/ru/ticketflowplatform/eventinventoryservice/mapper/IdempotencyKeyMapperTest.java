package ru.ticketflowplatform.eventinventoryservice.mapper;

import java.time.Instant;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Event;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.IdempotencyKey;

class IdempotencyKeyMapperTest {

    private final IdempotencyKeyMapper idempotencyKeyMapper = new IdempotencyKeyMapperImpl();

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.mapper.IdempotencyKeyMapperTestCases#toEntity_whenIdempotencyDataIsProvided_thenMapKeyTestCases")
    @DisplayName("Преобразует данные идемпотентности в сущность по сценариям")
    void toEntity_whenIdempotencyDataIsProvided_thenMapKeyTest(
        String scenario,
        String organizerId,
        String key,
        String requestHash,
        Event event,
        Instant now) {
        IdempotencyKey idempotencyKey = idempotencyKeyMapper.toEntity(organizerId, key, requestHash, event, now);

        Assertions.assertThat(idempotencyKey.getId()).isNull();
        Assertions.assertThat(idempotencyKey.getOrganizerId()).isEqualTo(organizerId);
        Assertions.assertThat(idempotencyKey.getKey()).isEqualTo(key);
        Assertions.assertThat(idempotencyKey.getRequestHash()).isEqualTo(requestHash);
        Assertions.assertThat(idempotencyKey.getEvent()).isSameAs(event);
        Assertions.assertThat(idempotencyKey.getCreatedAt()).isEqualTo(now);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.mapper.IdempotencyKeyMapperTestCases#toEntity_whenOneInputIsProvided_thenMapAvailableValueTestCases")
    @DisplayName("Преобразует частично заданные данные идемпотентности")
    void toEntity_whenOneInputIsProvided_thenMapAvailableValueTest(
        String scenario,
        String organizerId,
        String key,
        String requestHash,
        Event event,
        Instant now) {
        IdempotencyKey idempotencyKey = idempotencyKeyMapper.toEntity(organizerId, key, requestHash, event, now);

        Assertions.assertThat(idempotencyKey).isNotNull();
        Assertions.assertThat(idempotencyKey.getId()).isNull();
        Assertions.assertThat(idempotencyKey.getOrganizerId()).isEqualTo(organizerId);
        Assertions.assertThat(idempotencyKey.getKey()).isEqualTo(key);
        Assertions.assertThat(idempotencyKey.getRequestHash()).isEqualTo(requestHash);
        Assertions.assertThat(idempotencyKey.getEvent()).isSameAs(event);
        Assertions.assertThat(idempotencyKey.getCreatedAt()).isEqualTo(now);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.mapper.IdempotencyKeyMapperTestCases#toEntity_whenAllInputsAreNull_thenReturnNullTestCases")
    @DisplayName("Возвращает null, если данные идемпотентности не заданы")
    void toEntity_whenAllInputsAreNull_thenReturnNullTest(String scenario) {
        Assertions.assertThat(idempotencyKeyMapper.toEntity(null, null, null, null, null)).isNull();
    }
}
