package ru.ticketflowplatform.eventinventoryservice.config;

import com.sun.net.httpserver.HttpServer;
import helper.BaseIntegrationTest;
import helper.SecurityConfigHelper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.CreateEventResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.EventStatus;

@Sql(scripts = "/scenario/cases/sql/clear-event-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/scenario/cases/sql/clear-event-data.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class SecurityConfigApiTest extends BaseIntegrationTest {

    private static final HttpServer JWKS_SERVER = SecurityConfigHelper.startJwksServer();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void jwkSetUri(DynamicPropertyRegistry properties) {
        properties.add(
            "security.jwt.jwk-set-uri",
            () -> "http://127.0.0.1:" + JWKS_SERVER.getAddress().getPort() + "/jwks"
        );
    }

    @AfterAll
    static void stopJwksServer() {
        JWKS_SERVER.stop(0);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.config.SecurityConfigApiTestCases#post_whenOrganizerJwtIsValid_thenCreateDraftTestCases")
    @DisplayName("Преобразует realm_access.roles и создаёт черновик")
    void post_whenOrganizerJwtIsValid_thenCreateDraftTest(
        String scenario,
        String token,
        String requestBody,
        String idempotencyKey,
        String expectedOrganizerId,
        String expectedTitle) throws Exception {
        MvcResult result = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/events")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        ).andReturn();
        CreateEventResponseDto response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            CreateEventResponseDto.class
        );

        Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(201);
        Assertions.assertThat(response.id()).isNotNull();
        Assertions.assertThat(response.organizerId()).isEqualTo(expectedOrganizerId);
        Assertions.assertThat(response.title()).isEqualTo(expectedTitle);
        Assertions.assertThat(response.description()).isNull();
        Assertions.assertThat(response.categoryIds()).isEmpty();
        Assertions.assertThat(response.images()).isEmpty();
        Assertions.assertThat(response.ageRestriction()).isNull();
        Assertions.assertThat(response.status()).isEqualTo(EventStatus.DRAFT);
        Assertions.assertThat(response.createdAt()).isNotNull().isEqualTo(response.updatedAt());
        Assertions.assertThat(response.publishedAt()).isNull();
        Assertions.assertThat(response.cancelledAt()).isNull();
        Assertions.assertThat(response.completedAt()).isNull();
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.events", Integer.class)).isOne();
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.idempotency_keys", Integer.class)).isOne();
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.config.SecurityConfigApiTestCases#post_whenJwtDoesNotAuthorizeRequest_thenReturnCompleteErrorTestCases")
    @DisplayName("Возвращает полную ошибку для неверной роли, аудитории или издателя")
    void post_whenJwtDoesNotAuthorizeRequest_thenReturnCompleteErrorTest(
        String scenario,
        String token,
        String requestBody,
        String idempotencyKey,
        ErrorResponseDto expectedError) throws Exception {
        MvcResult result = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/events")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", idempotencyKey)
                .header("Accept-Language", "en")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        ).andReturn();
        ErrorResponseDto error = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponseDto.class);

        Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(expectedError.status());
        Assertions.assertThat(error).isEqualTo(expectedError);
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.events", Integer.class)).isZero();
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.idempotency_keys", Integer.class)).isZero();
    }

}
