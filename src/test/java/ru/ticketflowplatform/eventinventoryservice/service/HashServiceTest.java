package ru.ticketflowplatform.eventinventoryservice.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto;

class HashServiceTest {

    private final HashService hashService = new HashService();

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.HashServiceTestCases#hash_whenRequestIsProvided_shouldReturnSha256FingerprintTestCases")
    @DisplayName("Вычисляет SHA-256 отпечаток запроса по сценариям")
    void hash_whenRequestIsProvided_shouldReturnSha256FingerprintTest(
            String scenario,
            CreateEventRequestDto request,
            String expectedHash
    ) {
        Assertions.assertEquals(expectedHash, hashService.hash(request));
    }
}
