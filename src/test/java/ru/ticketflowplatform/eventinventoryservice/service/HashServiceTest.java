package ru.ticketflowplatform.eventinventoryservice.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.HashGenerationException;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

class HashServiceTest {

    private final HashService hashService = new HashService(JsonMapper.builder()
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .disable(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST)
        .build());

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.service.HashServiceTestCases#hash_whenRequestIsProvided_thenReturnSha256FingerprintTestCases")
    @DisplayName("Вычисляет SHA-256 отпечаток запроса по сценариям")
    void hash_whenRequestIsProvided_thenReturnSha256FingerprintTest(
        String scenario, CreateEventRequestDto request, String expectedHash) {
        Assertions.assertThat(hashService.hash(request)).isEqualTo(expectedHash);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.service.HashServiceTestCases#hash_whenRequestsHaveDifferentContent_thenReturnDifferentFingerprintsTestCases")
    @DisplayName("Различает границы полей, null и строковые значения")
    void hash_whenRequestsHaveDifferentContent_thenReturnDifferentFingerprintsTest(
        String scenario,
        CreateEventRequestDto firstRequest,
        CreateEventRequestDto secondRequest) {
        Assertions.assertThat(hashService.hash(firstRequest))
            .isNotEqualTo(hashService.hash(secondRequest));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.service.HashServiceTestCases#hash_whenSha256IsUnavailable_thenThrowDomainExceptionTestCases")
    @DisplayName("Преобразует недоступность SHA-256 в доменную ошибку")
    void hash_whenSha256IsUnavailable_thenThrowDomainExceptionTest(
        String scenario, CreateEventRequestDto request, NoSuchAlgorithmException failure)
        throws Exception {
        try (MockedStatic<MessageDigest> messageDigest = Mockito.mockStatic(MessageDigest.class)) {
            messageDigest.when(() -> MessageDigest.getInstance("SHA-256")).thenThrow(failure);

            Assertions.assertThatThrownBy(() -> hashService.hash(request))
                .isInstanceOfSatisfying(
                    HashGenerationException.class,
                    exception -> Assertions.assertThat(exception.getCause()).isSameAs(failure));
        }
    }
}
