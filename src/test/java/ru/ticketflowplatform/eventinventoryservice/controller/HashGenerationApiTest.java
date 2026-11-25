package ru.ticketflowplatform.eventinventoryservice.controller;

import helper.BaseIntegrationTest;
import java.security.NoSuchAlgorithmException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.HashGenerationException;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;
import ru.ticketflowplatform.eventinventoryservice.service.HashService;

@Sql(scripts = "/scenario/cases/sql/clear-event-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/scenario/cases/sql/clear-event-data.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class HashGenerationApiTest extends BaseIntegrationTest {

    @MockitoBean
    private HashService hashService;

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.controller.HashGenerationApiTestCases#create_whenRequestFingerprintCannotBeGenerated_thenReturnInternalErrorTestCases")
    @DisplayName("Возвращает полную ошибку API при невозможности создать отпечаток")
    void create_whenRequestFingerprintCannotBeGenerated_thenReturnInternalErrorTest(
        String scenario,
        String requestBody,
        String idempotencyKey,
        HashGenerationException failure,
        Throwable expectedCause,
        ErrorResponseDto expectedError) throws Exception {
        Mockito.when(hashService.hash(Mockito.any())).thenThrow(failure);

        MvcResult result = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/events")
                .with(
                    SecurityMockMvcRequestPostProcessors.jwt()
                        .jwt(jwt -> jwt.subject("organizer-1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZER"))
                )
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        ).andReturn();
        ErrorResponseDto error = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponseDto.class);

        Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(expectedError.status());
        Assertions.assertThat(error).isEqualTo(expectedError);
        Assertions.assertThat(failure.getCause()).isSameAs(expectedCause);
    }
}
