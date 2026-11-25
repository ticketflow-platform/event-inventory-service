package ru.ticketflowplatform.eventinventoryservice.controller;

import tools.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import helper.BaseIntegrationTest;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.CreateEventResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.CreateEventResponseDto.EventImageResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.EventStatus;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.ImagePosition;
import ru.ticketflowplatform.eventinventoryservice.service.EventService;

@Sql(scripts = "/scenario/cases/sql/clear-event-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/scenario/cases/sql/clear-event-data.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class EventControllerTest extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private EventService eventService;

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.controller.EventControllerTestCases#create_whenServiceFails_thenReturnCompleteErrorTestCases")
    @DisplayName("Возвращает полную HTTP-ошибку при сбое сервиса")
    void create_whenServiceFails_thenReturnCompleteErrorTest(
        String scenario,
        String requestBody,
        String idempotencyKey,
        String language,
        RuntimeException failure,
        ErrorResponseDto expectedError) throws Exception {
        Mockito.doThrow(failure).when(eventService).create(
            Mockito.any(CreateEventRequestDto.class),
            Mockito.eq("organizer-1"),
            Mockito.eq(idempotencyKey)
        );

        MvcResult result = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/events")
                .with(
                    SecurityMockMvcRequestPostProcessors.jwt()
                        .jwt(jwt -> jwt.subject("organizer-1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZER"))
                )
                .header("Idempotency-Key", idempotencyKey)
                .header("Accept-Language", language)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        ).andReturn();
        ErrorResponseDto error = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponseDto.class);

        Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(expectedError.status());
        Assertions.assertThat(error).isEqualTo(expectedError);
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.events", Integer.class)).isZero();
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.idempotency_keys", Integer.class)).isZero();
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.controller.EventControllerTestCases#openApi_whenLocaleIsRequested_thenDocumentSchemaAndLocalizedSpecTestCases")
    @DisplayName("Документирует схему запроса и локализует OpenAPI по сценариям")
    void openApi_whenLocaleIsRequested_thenDocumentSchemaAndLocalizedSpecTest(
        String scenario,
        String openApiPath,
        String expectedInfoTitle,
        String expectedTitleDescription,
        String expectedOperationSummary,
        String expectedResponseDescription) throws Exception {
        MvcResult result = mockMvc.perform(
            MockMvcRequestBuilders.get(openApiPath)
                .with(
                    SecurityMockMvcRequestPostProcessors.jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))
                )
        ).andReturn();
        JsonNode openApi = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode schema = openApi.at("/components/schemas/CreateEventRequestDto");

        Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(200);
        Assertions.assertThat(schema.path("required").toString()).isEqualTo("[\"title\"]");
        Assertions.assertThat(schema.path("properties").path("ageRestriction").path("example").asString()).isEqualTo("0");
        Assertions.assertThat(schema.path("properties").path("ageRestriction").path("enum").toString())
            .isEqualTo("[\"0\",\"6\",\"12\",\"16\",\"18\"]");
        Assertions.assertThat(schema.path("properties").path("title").path("description").asString())
            .isEqualTo(expectedTitleDescription);
        Assertions.assertThat(openApi.path("info").path("title").asString()).isEqualTo(expectedInfoTitle);
        Assertions.assertThat(openApi.at("/paths/~1api~1v1~1events/post/summary").asString()).isEqualTo(expectedOperationSummary);
        Assertions.assertThat(openApi.at("/paths/~1api~1v1~1events/post/responses/500/description").asString())
            .isEqualTo(expectedResponseDescription);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.controller.EventControllerTestCases#health_whenRequestIsUnauthenticated_thenAllowAccessTestCases")
    @DisplayName("Не требует аутентификации для health")
    void health_whenRequestIsUnauthenticated_thenAllowAccessTest(
        String scenario,
        String path,
        int expectedStatus,
        String expectedBody) throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get(path)).andReturn();

        Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(expectedStatus);
        Assertions.assertThat(objectMapper.readTree(result.getResponse().getContentAsString()))
            .isEqualTo(objectMapper.readTree(expectedBody));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.controller.EventControllerTestCases#developerEndpoints_whenPathIsRequested_thenRequireDeveloperRoleTestCases")
    @DisplayName("Требует роль DEVELOPER для служебных endpoint-ов по сценариям")
    void developerEndpoints_whenPathIsRequested_thenRequireDeveloperRoleTest(
        String scenario,
        String path,
        int expectedUnauthenticatedStatus,
        int expectedForbiddenStatus) throws Exception {
        Assertions.assertThat(mockMvc.perform(MockMvcRequestBuilders.get(path)).andReturn().getResponse().getStatus())
            .as(path)
            .isEqualTo(expectedUnauthenticatedStatus);
        Assertions.assertThat(
            mockMvc.perform(
                MockMvcRequestBuilders.get(path)
                    .with(
                        SecurityMockMvcRequestPostProcessors.jwt()
                            .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZER"))
                    )
            )
                .andReturn().getResponse().getStatus()
        )
            .as(path)
            .isEqualTo(expectedForbiddenStatus);
        int developerStatus = mockMvc.perform(
            MockMvcRequestBuilders.get(path)
                .with(
                    SecurityMockMvcRequestPostProcessors.jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))
                )
        )
            .andReturn().getResponse().getStatus();
        Assertions.assertThat(developerStatus).as(path).isNotEqualTo(401).isNotEqualTo(403);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.controller.EventControllerTestCases#create_whenOrganizerSubmitsValidDraft_thenReturnCompleteResponseTestCases")
    @DisplayName("Создаёт черновик и сохраняет полный ответ и связи в БД")
    @Sql("/scenario/cases/sql/seed-event-data.sql")
    void create_whenOrganizerSubmitsValidDraft_thenReturnCompleteResponseTest(
        String scenario,
        String requestBody,
        String idempotencyKey,
        String expectedTitle,
        String expectedDescription,
        Integer expectedAgeRestriction,
        List<UUID> expectedCategoryIds,
        List<UUID> expectedImageIds,
        List<ImagePosition> expectedImagePositions) throws Exception {
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
        CreateEventResponseDto response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            CreateEventResponseDto.class
        );

        Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(201);
        Assertions.assertThat(response.id()).isNotNull();
        Assertions.assertThat(response.organizerId()).isEqualTo("organizer-1");
        Assertions.assertThat(response.title()).isEqualTo(expectedTitle);
        Assertions.assertThat(response.description()).isEqualTo(expectedDescription);
        Assertions.assertThat(response.categoryIds()).containsExactlyElementsOf(expectedCategoryIds);
        Assertions.assertThat(response.images()).extracting(EventImageResponseDto::imageId)
            .containsExactlyElementsOf(expectedImageIds);
        Assertions.assertThat(response.images()).extracting(EventImageResponseDto::type)
            .containsExactlyElementsOf(expectedImagePositions);
        Assertions.assertThat(response.ageRestriction()).isEqualTo(expectedAgeRestriction);
        Assertions.assertThat(response.status()).isEqualTo(EventStatus.DRAFT);
        Assertions.assertThat(response.createdAt()).isNotNull().isEqualTo(response.updatedAt());
        Assertions.assertThat(response.images()).extracting(EventImageResponseDto::createdAt)
            .containsOnly(response.createdAt());
        Assertions.assertThat(response.publishedAt()).isNull();
        Assertions.assertThat(response.cancelledAt()).isNull();
        Assertions.assertThat(response.completedAt()).isNull();
        Assertions.assertThat(
            jdbcTemplate.queryForObject(
                "SELECT organizer_id FROM events.events WHERE id = ?",
                String.class,
                response.id()
            )
        ).isEqualTo("organizer-1");
        Assertions.assertThat(
            jdbcTemplate.queryForObject(
                "SELECT status FROM events.events WHERE id = ?",
                String.class,
                response.id()
            )
        ).isEqualTo("DRAFT");
        Assertions.assertThat(
            jdbcTemplate.queryForList(
                "SELECT category_id FROM events.events_categories WHERE event_id = ? ORDER BY category_id",
                UUID.class,
                response.id()
            )
        ).containsExactlyElementsOf(expectedCategoryIds);
        Assertions.assertThat(
            jdbcTemplate.queryForList(
                "SELECT image_id FROM events.events_images WHERE event_id = ? ORDER BY type, image_id",
                UUID.class,
                response.id()
            )
        ).containsExactlyElementsOf(expectedImageIds);
        Assertions.assertThat(
            jdbcTemplate.queryForList(
                "SELECT type FROM events.events_images WHERE event_id = ? ORDER BY type, image_id",
                String.class,
                response.id()
            )
        ).containsExactlyElementsOf(expectedImagePositions.stream().map(Enum::name).toList());
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.events", Integer.class)).isOne();
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.idempotency_keys", Integer.class)).isOne();
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.controller.EventControllerTestCases#create_whenOrganizerSubmitsInvalidRequest_thenReturnCompleteErrorTestCases")
    @DisplayName("Не создаёт событие при недопустимых данных или ссылках")
    @Sql("/scenario/cases/sql/seed-event-data.sql")
    void create_whenOrganizerSubmitsInvalidRequest_thenReturnCompleteErrorTest(
        String scenario,
        String requestBody,
        String idempotencyKey,
        ErrorResponseDto expectedError) throws Exception {
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
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.events", Integer.class)).isZero();
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.events_categories", Integer.class)).isZero();
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.events_images", Integer.class)).isZero();
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.idempotency_keys", Integer.class)).isZero();
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.controller.EventControllerTestCases#create_whenJsonIsMalformed_thenReturnBadRequestTestCases")
    @DisplayName("Возвращает ошибку для некорректного JSON")
    void create_whenJsonIsMalformed_thenReturnBadRequestTest(
        String scenario,
        String requestBody,
        ErrorResponseDto expectedError) throws Exception {
        MvcResult result = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/events")
                .with(
                    SecurityMockMvcRequestPostProcessors.jwt()
                        .jwt(jwt -> jwt.subject("organizer-1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZER"))
                )
                .header("Idempotency-Key", "malformed-json")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        ).andReturn();
        ErrorResponseDto error = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponseDto.class);

        Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(expectedError.status());
        Assertions.assertThat(error).isEqualTo(expectedError);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.controller.EventControllerTestCases#create_whenRequestIsUnauthenticated_thenReturnUnauthorizedTestCases")
    @DisplayName("Отклоняет неаутентифицированный запрос по сценариям")
    void create_whenRequestIsUnauthenticated_thenReturnUnauthorizedTest(
        String scenario,
        String requestBody,
        String language,
        ErrorResponseDto expectedError) throws Exception {
        MvcResult result = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/events")
                    .header("Idempotency-Key", "unauthenticated-key")
                    .header("Accept-Language", language)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andReturn();

        ErrorResponseDto error = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponseDto.class);
        Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(expectedError.status());
        Assertions.assertThat(error).isEqualTo(expectedError);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.controller.EventControllerTestCases#create_whenBearerTokenIsInvalid_thenReturnUnauthorizedErrorTestCases")
    @DisplayName("Возвращает стандартную ошибку для недействительного bearer-токена по сценариям")
    void create_whenBearerTokenIsInvalid_thenReturnUnauthorizedErrorTest(
        String scenario,
        String token,
        String expectedChallenge,
        ErrorResponseDto expectedError) throws Exception {
        MvcResult result = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/events")
                    .header("Authorization", "Bearer " + token)
                    .header("Accept-Language", "ru")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
            )
            .andReturn();

        Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(expectedError.status());
        Assertions.assertThat(result.getResponse().getHeader("WWW-Authenticate")).startsWith(expectedChallenge);
        ErrorResponseDto error = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponseDto.class);
        Assertions.assertThat(error).isEqualTo(expectedError);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.controller.EventControllerTestCases#create_whenUserLacksOrganizerRole_thenReturnForbiddenTestCases")
    @DisplayName("Отклоняет пользователя без роли организатора по сценариям")
    void create_whenUserLacksOrganizerRole_thenReturnForbiddenTest(
        String scenario,
        String requestBody,
        ErrorResponseDto expectedError) throws Exception {
        MvcResult result = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/events")
                    .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("user-1")))
                    .header("Idempotency-Key", "non-organizer-key")
                    .header("Accept-Language", "ru")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andReturn();

        ErrorResponseDto error = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponseDto.class);
        Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(expectedError.status());
        Assertions.assertThat(error).isEqualTo(expectedError);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.controller.EventControllerTestCases#create_whenOptionalDraftFieldsAreOmitted_thenCreateDraftTestCases")
    @DisplayName("Создает черновик с любыми необязательными полями")
    void create_whenOptionalDraftFieldsAreOmitted_thenCreateDraftTest(
        String scenario,
        String requestBody,
        String idempotencyKey) throws Exception {
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
        CreateEventResponseDto response = objectMapper.readValue(result.getResponse().getContentAsString(), CreateEventResponseDto.class);

        Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(201);
        Assertions.assertThat(response.id()).isNotNull();
        Assertions.assertThat(response.organizerId()).isEqualTo("organizer-1");
        Assertions.assertThat(response.title()).isEqualTo("Festival");
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
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.events_categories", Integer.class)).isZero();
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.events_images", Integer.class)).isZero();
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.idempotency_keys", Integer.class)).isOne();
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.controller.EventControllerTestCases#create_whenBusinessRuleIsViolated_thenReturnBadRequestTestCases")
    @DisplayName("Показывает значение неверного возраста и требует одну обложку")
    void create_whenBusinessRuleIsViolated_thenReturnBadRequestTest(
        String scenario,
        String requestBody,
        String idempotencyKey,
        ErrorResponseDto expectedError) throws Exception {
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
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.events", Integer.class)).isZero();
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.idempotency_keys", Integer.class)).isZero();
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.controller.EventControllerTestCases#create_whenIdempotencyHeaderIsMissing_thenReturnLocalizedBadRequestTestCases")
    @DisplayName("Локализует ошибку отсутствующего Idempotency-Key по сценариям")
    void create_whenIdempotencyHeaderIsMissing_thenReturnLocalizedBadRequestTest(
        String scenario,
        String requestBody,
        String language,
        ErrorResponseDto expectedError) throws Exception {
        MvcResult result = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/events")
                    .with(
                        SecurityMockMvcRequestPostProcessors.jwt()
                            .jwt(jwt -> jwt.subject("organizer-1"))
                            .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZER"))
                    )
                    .header("Accept-Language", language)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andReturn();

        ErrorResponseDto error = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponseDto.class);
        Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(expectedError.status());
        Assertions.assertThat(error).isEqualTo(expectedError);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.controller.EventControllerTestCases#create_whenIdempotencyHeaderIsBlank_thenReturnBadRequestTestCases")
    @DisplayName("Отклоняет пустой Idempotency-Key по сценариям")
    void create_whenIdempotencyHeaderIsBlank_thenReturnBadRequestTest(
        String scenario,
        String idempotencyKey,
        String requestBody,
        ErrorResponseDto expectedError)
        throws Exception {
        MvcResult result = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/events")
                    .with(
                        SecurityMockMvcRequestPostProcessors.jwt()
                            .jwt(jwt -> jwt.subject("organizer-1"))
                            .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZER"))
                    )
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andReturn();

        ErrorResponseDto error = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponseDto.class);
        Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(expectedError.status());
        Assertions.assertThat(error).isEqualTo(expectedError);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.controller.EventControllerTestCases#create_whenRequestIsRetried_thenCreateOnlyOneEventTestCases")
    @DisplayName("Повтор запроса с тем же ключом не дублирует событие по сценариям")
    @Sql("/scenario/cases/sql/seed-event-data.sql")
    @Sql(
        scripts = "/scenario/cases/sql/clear-event-data.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void create_whenRequestIsRetried_thenCreateOnlyOneEventTest(String scenario, String requestBody, String idempotencyKey)
        throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post("/api/v1/events")
            .with(
                SecurityMockMvcRequestPostProcessors.jwt()
                    .jwt(jwt -> jwt.subject("organizer-1"))
                    .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZER"))
            )
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody);
        MvcResult firstResult = mockMvc.perform(request).andReturn();
        MvcResult retryResult = mockMvc.perform(request).andReturn();

        Assertions.assertThat(firstResult.getResponse().getStatus()).isEqualTo(201);
        Assertions.assertThat(retryResult.getResponse().getStatus()).isEqualTo(201);
        CreateEventResponseDto firstResponse = objectMapper.readValue(
            firstResult.getResponse().getContentAsString(),
            CreateEventResponseDto.class
        );
        CreateEventResponseDto retryResponse = objectMapper.readValue(
            retryResult.getResponse().getContentAsString(),
            CreateEventResponseDto.class
        );
        Assertions.assertThat(retryResponse).isEqualTo(firstResponse);
        Assertions.assertThat(firstResponse.categoryIds()).hasSize(1);
        Assertions.assertThat(firstResponse.images()).extracting(EventImageResponseDto::type)
            .containsExactly(ImagePosition.COVER, ImagePosition.GALLERY);
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.events", Integer.class)).isOne();
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.events_categories", Integer.class)).isOne();
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.events_images", Integer.class)).isEqualTo(2);
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.idempotency_keys", Integer.class)).isOne();
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.controller.EventControllerTestCases#create_whenIdempotencyKeyIsReusedForDifferentRequest_thenReturnConflictTestCases")
    @DisplayName("Отклоняет повтор ключа с другим запросом по сценариям")
    @Sql("/scenario/cases/sql/seed-event-data.sql")
    @Sql(
        scripts = "/scenario/cases/sql/clear-event-data.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void create_whenIdempotencyKeyIsReusedForDifferentRequest_thenReturnConflictTest(
        String scenario,
        String firstRequest,
        String changedRequest,
        String idempotencyKey,
        int expectedCreatedStatus,
        ErrorResponseDto expectedConflict) throws Exception {
        MvcResult firstResult = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/events")
                    .with(
                        SecurityMockMvcRequestPostProcessors.jwt()
                            .jwt(jwt -> jwt.subject("organizer-1"))
                            .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZER"))
                    )
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(firstRequest)
            )
            .andReturn();
        MvcResult conflictResult = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/events")
                    .with(
                        SecurityMockMvcRequestPostProcessors.jwt()
                            .jwt(jwt -> jwt.subject("organizer-1"))
                            .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZER"))
                    )
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(changedRequest)
            )
            .andReturn();

        Assertions.assertThat(firstResult.getResponse().getStatus()).isEqualTo(expectedCreatedStatus);
        CreateEventResponseDto firstResponse = objectMapper.readValue(
            firstResult.getResponse().getContentAsString(),
            CreateEventResponseDto.class
        );
        ErrorResponseDto error = objectMapper.readValue(conflictResult.getResponse().getContentAsString(), ErrorResponseDto.class);
        Assertions.assertThat(conflictResult.getResponse().getStatus()).isEqualTo(expectedConflict.status());
        Assertions.assertThat(error).isEqualTo(expectedConflict);
        Assertions.assertThat(firstResponse.id()).isNotNull();
        Assertions.assertThat(firstResponse.organizerId()).isEqualTo("organizer-1");
        Assertions.assertThat(firstResponse.title()).isEqualTo("Festival");
        Assertions.assertThat(firstResponse.categoryIds()).containsExactly(
            UUID.fromString("00000000-0000-0000-0000-000000000011")
        );
        Assertions.assertThat(firstResponse.images()).extracting(EventImageResponseDto::type)
            .containsExactly(ImagePosition.COVER);
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.events", Integer.class)).isOne();
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.events_categories", Integer.class)).isOne();
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.events_images", Integer.class)).isOne();
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.idempotency_keys", Integer.class)).isOne();
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.controller.EventControllerTestCases#create_whenOneHundredCategoriesAreRequested_thenPersistAllLinksTestCases")
    @DisplayName("Сохраняет связи для ста категорий")
    @Sql("/scenario/cases/sql/seed-hundred-categories.sql")
    @Sql(
        scripts = "/scenario/cases/sql/clear-event-data.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void create_whenOneHundredCategoriesAreRequested_thenPersistAllLinksTest(
        String scenario,
        String requestBody,
        String idempotencyKey,
        List<UUID> expectedCategoryIds)
        throws Exception {
        MvcResult result = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/events")
                    .with(
                        SecurityMockMvcRequestPostProcessors.jwt()
                            .jwt(jwt -> jwt.subject("organizer-1"))
                            .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZER"))
                    )
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andReturn();

        CreateEventResponseDto response = objectMapper.readValue(result.getResponse().getContentAsString(), CreateEventResponseDto.class);

        Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(201);
        Assertions.assertThat(response.categoryIds()).containsExactlyInAnyOrderElementsOf(expectedCategoryIds);
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.events_categories", Integer.class))
            .isEqualTo(expectedCategoryIds.size());
        Assertions.assertThat(
            jdbcTemplate.queryForList(
                "SELECT category_id FROM events.events_categories ORDER BY category_id",
                UUID.class
            )
        ).containsExactlyElementsOf(expectedCategoryIds);
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.events_images", Integer.class)).isOne();
    }
}
