package ru.ticketflowplatform.eventinventoryservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class EventControllerTest {

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Документирует обязательные поля и возрастные значения в OpenAPI")
    void openApi_shouldDocumentRequiredRequestFields() throws Exception {
        MvcResult result = mockMvc.perform(getWithRole("/v3/api-docs", "DEVELOPER")).andReturn();
        JsonNode schema = new ObjectMapper().readTree(result.getResponse().getContentAsString())
                .at("/components/schemas/CreateEventRequestDto");

        Assertions.assertEquals(200, result.getResponse().getStatus());
        Assertions.assertTrue(schema.path("required").toString().contains("title"));
        Assertions.assertFalse(schema.path("required").toString().contains("categoryIds"));
        Assertions.assertFalse(schema.path("required").toString().contains("images"));
        Assertions.assertFalse(schema.path("required").toString().contains("ageRestriction"));
        Assertions.assertEquals("0", schema.path("properties").path("ageRestriction").path("example").asText());
        Assertions.assertEquals(
                "[\"0\",\"6\",\"12\",\"16\",\"18\"]",
                schema.path("properties").path("ageRestriction").path("enum").toString());
        Assertions.assertEquals(
                "Название мероприятия. Единственное обязательное поле при создании черновика.",
                schema.path("properties").path("title").path("description").asText());
    }

    @Test
    @DisplayName("Отдаёт отдельные локализованные спецификации OpenAPI")
    void openApi_shouldExposeRussianAndEnglishSpecs() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        MvcResult russian = mockMvc.perform(getWithRole("/v3/api-docs/ru", "DEVELOPER")).andReturn();
        MvcResult english = mockMvc.perform(getWithRole("/v3/api-docs/en", "DEVELOPER")).andReturn();

        JsonNode russianSpec = objectMapper.readTree(russian.getResponse().getContentAsString());
        JsonNode englishSpec = objectMapper.readTree(english.getResponse().getContentAsString());

        Assertions.assertEquals(200, russian.getResponse().getStatus());
        Assertions.assertEquals(200, english.getResponse().getStatus());
        Assertions.assertEquals("TicketFlow — API мероприятий", russianSpec.path("info").path("title").asText());
        Assertions.assertEquals("TicketFlow Events API", englishSpec.path("info").path("title").asText());
        Assertions.assertEquals("Create an event", englishSpec.at("/paths/~1api~1v1~1events/post/summary").asText());
        Assertions.assertEquals(
                "Внутренняя ошибка при обработке запроса",
                russianSpec.at("/paths/~1api~1v1~1events/post/responses/500/description").asText());
        Assertions.assertEquals(
                "Internal error while processing the request",
                englishSpec.at("/paths/~1api~1v1~1events/post/responses/500/description").asText());
    }

    @Test
    @DisplayName("Ограничивает health, Swagger и OpenAPI ролью DEVELOPER")
    void developerEndpoints_shouldRequireDeveloperRole() throws Exception {
        for (String path : new String[] {
                "/actuator/health",
                "/swagger-ui.html",
                "/swagger-ui/index.html",
                "/v3/api-docs/ru"
        }) {
            Assertions.assertEquals(401, mockMvc.perform(MockMvcRequestBuilders.get(path)).andReturn()
                    .getResponse().getStatus(), path);
            Assertions.assertEquals(403, mockMvc.perform(getWithRole(path, "ORGANIZER")).andReturn()
                    .getResponse().getStatus(), path);
            int developerStatus = mockMvc.perform(getWithRole(path, "DEVELOPER")).andReturn()
                    .getResponse().getStatus();
            Assertions.assertNotEquals(401, developerStatus, path);
            Assertions.assertNotEquals(403, developerStatus, path);
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventControllerTestCases#create_whenOrganizerSubmitsRequest_shouldReturnExpectedResultTestCases")
    @DisplayName("Создаёт событие или возвращает ошибку по сценариям")
    @Sql(
            statements = {
                    "DELETE FROM events.idempotency_keys",
                    "DELETE FROM events.events_images",
                    "DELETE FROM events.events_categories",
                    "DELETE FROM events.events",
                    "DELETE FROM events.images",
                    "DELETE FROM events.categories",
                    "INSERT INTO events.categories (id, name, status, created_at) VALUES ('00000000-0000-0000-0000-000000000011', 'Music', 'ACTIVE', CURRENT_TIMESTAMP)",
                    "INSERT INTO events.images (id, organizer_id, storage_key, content_type, file_size, created_at) VALUES ('00000000-0000-0000-0000-000000000012', 'organizer-1', 'events/cover.jpg', 'image/jpeg', 1024, CURRENT_TIMESTAMP)" })
    @Sql(
            statements = {
                    "DELETE FROM events.idempotency_keys",
                    "DELETE FROM events.events_images",
                    "DELETE FROM events.events_categories",
                    "DELETE FROM events.events",
                    "DELETE FROM events.images",
                    "DELETE FROM events.categories" },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void create_whenOrganizerSubmitsRequest_shouldReturnExpectedResultTest(
            String scenario,
            String requestBody,
            String idempotencyKey,
            int expectedStatus,
            int expectedEventCount,
            String expectedBodyPart) throws Exception {
        MvcResult result = mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/api/v1/events")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt()
                                                .jwt(jwt -> jwt.subject("organizer-1"))
                                                .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZER")))
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andReturn();

        Assertions.assertEquals(expectedStatus, result.getResponse().getStatus());
        Assertions.assertTrue(result.getResponse().getContentAsString().contains(expectedBodyPart));
        Assertions.assertEquals(expectedEventCount, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.events", Integer.class));
    }

    @Test
    @DisplayName("Не позволяет добавить неактивную категорию к мероприятию")
    @Sql(
            statements = {
                    "DELETE FROM events.idempotency_keys",
                    "DELETE FROM events.events_images",
                    "DELETE FROM events.events_categories",
                    "DELETE FROM events.events",
                    "DELETE FROM events.images",
                    "DELETE FROM events.categories",
                    "INSERT INTO events.categories (id, name, status, created_at) VALUES ('00000000-0000-0000-0000-000000000011', 'Inactive', 'INACTIVE', CURRENT_TIMESTAMP)" })
    @Sql(
            statements = {
                    "DELETE FROM events.idempotency_keys",
                    "DELETE FROM events.events_images",
                    "DELETE FROM events.events_categories",
                    "DELETE FROM events.events",
                    "DELETE FROM events.images",
                    "DELETE FROM events.categories" },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void create_whenCategoryIsInactive_shouldRejectAndNotPersistEvent() throws Exception {
        MvcResult result = mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/api/v1/events")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt()
                                                .jwt(jwt -> jwt.subject("organizer-1"))
                                                .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZER")))
                                .header("Idempotency-Key", "inactive-category-key")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"title":"Concert","categoryIds":[{"id":"00000000-0000-0000-0000-000000000011"}]}
                                        """))
                .andReturn();

        Assertions.assertEquals(422, result.getResponse().getStatus());
        Assertions.assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.events", Integer.class));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventControllerTestCases#create_whenRequestIsUnauthenticated_shouldReturnUnauthorizedTestCases")
    @DisplayName("Отклоняет неаутентифицированный запрос по сценариям")
    void create_whenRequestIsUnauthenticated_shouldReturnUnauthorizedTest(String scenario, String requestBody) throws Exception {
        MvcResult result = mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/api/v1/events")
                                .header("Idempotency-Key", "unauthenticated-key")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andReturn();

        Assertions.assertEquals(401, result.getResponse().getStatus());
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventControllerTestCases#create_whenUserLacksOrganizerRole_shouldReturnForbiddenTestCases")
    @DisplayName("Отклоняет пользователя без роли организатора по сценариям")
    void create_whenUserLacksOrganizerRole_shouldReturnForbiddenTest(String scenario, String requestBody) throws Exception {
        MvcResult result = mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/api/v1/events")
                                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("user-1")))
                                .header("Idempotency-Key", "non-organizer-key")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andReturn();

        Assertions.assertEquals(403, result.getResponse().getStatus());
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventControllerTestCases#create_whenCategoryIdsAreMissingOrEmpty_shouldReturnBadRequestTestCases")
    @DisplayName("Создает черновик без категории по сценариям")
    void create_whenCategoryIdsAreMissingOrEmpty_shouldReturnCreatedTest(String scenario, String requestBody) throws Exception {
        MvcResult result = mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/api/v1/events")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt()
                                                .jwt(jwt -> jwt.subject("organizer-1"))
                                                .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZER")))
                                .header("Idempotency-Key", "category-optional-" + scenario)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andReturn();

        Assertions.assertEquals(201, result.getResponse().getStatus());
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventControllerTestCases#create_whenImagesAreMissingOrEmpty_shouldReturnBadRequestTestCases")
    @DisplayName("Создает черновик без изображения по сценариям")
    void create_whenImagesAreMissingOrEmpty_shouldReturnCreatedTest(String scenario, String requestBody) throws Exception {
        MvcResult result = mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/api/v1/events")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt()
                                                .jwt(jwt -> jwt.subject("organizer-1"))
                                                .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZER")))
                                .header("Idempotency-Key", "image-optional-" + scenario)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andReturn();

        Assertions.assertEquals(201, result.getResponse().getStatus());
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventControllerTestCases#create_whenAgeRestrictionIsMissing_shouldReturnBadRequestTestCases")
    @DisplayName("Создает черновик без возрастного ограничения по сценариям")
    void create_whenAgeRestrictionIsMissing_shouldReturnCreatedTest(String scenario, String requestBody) throws Exception {
        MvcResult result = mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/api/v1/events")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt()
                                                .jwt(jwt -> jwt.subject("organizer-1"))
                                                .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZER")))
                                .header("Idempotency-Key", "age-optional-" + scenario)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andReturn();

        Assertions.assertEquals(201, result.getResponse().getStatus());
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventControllerTestCases#create_whenIdempotencyHeaderIsMissing_shouldReturnBadRequestTestCases")
    @DisplayName("Требует заголовок Idempotency-Key по сценариям")
    void create_whenIdempotencyHeaderIsMissing_shouldReturnBadRequestTest(String scenario, String requestBody) throws Exception {
        MvcResult result = mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/api/v1/events")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt()
                                                .jwt(jwt -> jwt.subject("organizer-1"))
                                                .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZER")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andReturn();

        Assertions.assertEquals(400, result.getResponse().getStatus());
        Assertions.assertTrue(result.getResponse().getContentAsString().contains("Отсутствует обязательный заголовок: Idempotency-Key"));
    }

    @Test
    @DisplayName("Локализует ошибки по Accept-Language")
    void create_whenHeaderIsMissingAndLanguageIsEnglish_shouldReturnEnglishError() throws Exception {
        MvcResult result = mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/api/v1/events")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt()
                                                .jwt(jwt -> jwt.subject("organizer-1"))
                                                .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZER")))
                                .header("Accept-Language", "en")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"Festival\"}"))
                .andReturn();

        Assertions.assertEquals(400, result.getResponse().getStatus());
        Assertions.assertTrue(result.getResponse().getContentAsString().contains("Missing required header: Idempotency-Key"));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventControllerTestCases#create_whenIdempotencyHeaderIsBlank_shouldReturnBadRequestTestCases")
    @DisplayName("Отклоняет пустой Idempotency-Key по сценариям")
    void create_whenIdempotencyHeaderIsBlank_shouldReturnBadRequestTest(String scenario, String idempotencyKey, String requestBody)
            throws Exception {
        MvcResult result = mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/api/v1/events")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt()
                                                .jwt(jwt -> jwt.subject("organizer-1"))
                                                .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZER")))
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andReturn();

        Assertions.assertEquals(400, result.getResponse().getStatus());
        Assertions.assertTrue(result.getResponse().getContentAsString().contains("Проверка запроса не пройдена"));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventControllerTestCases#create_whenRequestIsRetried_shouldCreateOnlyOneEventTestCases")
    @DisplayName("Повтор запроса с тем же ключом не дублирует событие по сценариям")
    @Sql(
            statements = {
                    "DELETE FROM events.idempotency_keys",
                    "DELETE FROM events.events_images",
                    "DELETE FROM events.events_categories",
                    "DELETE FROM events.events",
                    "DELETE FROM events.images",
                    "DELETE FROM events.categories",
                    "INSERT INTO events.categories (id, name, status, created_at) VALUES ('00000000-0000-0000-0000-000000000011', 'Music', 'ACTIVE', CURRENT_TIMESTAMP)",
                    "INSERT INTO events.images (id, organizer_id, storage_key, content_type, file_size, created_at) VALUES ('00000000-0000-0000-0000-000000000012', 'organizer-1', 'events/cover.jpg', 'image/jpeg', 1024, CURRENT_TIMESTAMP)" })
    @Sql(
            statements = {
                    "DELETE FROM events.idempotency_keys",
                    "DELETE FROM events.events_images",
                    "DELETE FROM events.events_categories",
                    "DELETE FROM events.events",
                    "DELETE FROM events.images",
                    "DELETE FROM events.categories" },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void create_whenRequestIsRetried_shouldCreateOnlyOneEventTest(String scenario, String requestBody, String idempotencyKey)
            throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post("/api/v1/events")
                .with(
                        SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject("organizer-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZER")))
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody);
        MvcResult firstResult = mockMvc.perform(request).andReturn();
        MvcResult retryResult = mockMvc.perform(request).andReturn();

        Assertions.assertEquals(201, firstResult.getResponse().getStatus());
        Assertions.assertEquals(201, retryResult.getResponse().getStatus());
        Assertions.assertEquals(firstResult.getResponse().getContentAsString(), retryResult.getResponse().getContentAsString());
        Assertions.assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.events", Integer.class));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventControllerTestCases#create_whenIdempotencyKeyIsReusedForDifferentRequest_shouldReturnConflictTestCases")
    @DisplayName("Отклоняет повтор ключа с другим запросом по сценариям")
    @Sql(
            statements = {
                    "DELETE FROM events.idempotency_keys",
                    "DELETE FROM events.events_images",
                    "DELETE FROM events.events_categories",
                    "DELETE FROM events.events",
                    "DELETE FROM events.images",
                    "DELETE FROM events.categories",
                    "INSERT INTO events.categories (id, name, status, created_at) VALUES ('00000000-0000-0000-0000-000000000011', 'Music', 'ACTIVE', CURRENT_TIMESTAMP)",
                    "INSERT INTO events.images (id, organizer_id, storage_key, content_type, file_size, created_at) VALUES ('00000000-0000-0000-0000-000000000012', 'organizer-1', 'events/cover.jpg', 'image/jpeg', 1024, CURRENT_TIMESTAMP)" })
    @Sql(
            statements = {
                    "DELETE FROM events.idempotency_keys",
                    "DELETE FROM events.events_images",
                    "DELETE FROM events.events_categories",
                    "DELETE FROM events.events",
                    "DELETE FROM events.images",
                    "DELETE FROM events.categories" },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void create_whenIdempotencyKeyIsReusedForDifferentRequest_shouldReturnConflictTest(
            String scenario,
            String firstRequest,
            String changedRequest,
            String idempotencyKey) throws Exception {
        MvcResult firstResult = mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/api/v1/events")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt()
                                                .jwt(jwt -> jwt.subject("organizer-1"))
                                                .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZER")))
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(firstRequest))
                .andReturn();
        MvcResult conflictResult = mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/api/v1/events")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt()
                                                .jwt(jwt -> jwt.subject("organizer-1"))
                                                .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZER")))
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(changedRequest))
                .andReturn();

        Assertions.assertEquals(201, firstResult.getResponse().getStatus());
        Assertions.assertEquals(409, conflictResult.getResponse().getStatus());
        Assertions.assertTrue(conflictResult.getResponse().getContentAsString().contains("Конфликт ключа идемпотентности"));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventControllerTestCases#create_whenOneHundredCategoriesAreRequested_shouldPersistAllLinksTestCases")
    @DisplayName("Сохраняет сто связей категорий пакетно в одной транзакции по сценариям")
    @Sql(
            statements = {
                    "DELETE FROM events.idempotency_keys",
                    "DELETE FROM events.events_images",
                    "DELETE FROM events.events_categories",
                    "DELETE FROM events.events",
                    "DELETE FROM events.images",
                    "DELETE FROM events.categories",
                    "INSERT INTO events.categories (id, name, status, created_at) SELECT ('00000000-0000-0000-0000-' || LPAD(category_number::text, 12, '0'))::uuid, 'Category ' || category_number, 'ACTIVE', CURRENT_TIMESTAMP FROM generate_series(1, 100) AS series(category_number)",
                    "INSERT INTO events.images (id, organizer_id, storage_key, content_type, file_size, created_at) VALUES ('00000000-0000-0000-0000-000000000012', 'organizer-1', 'events/cover.jpg', 'image/jpeg', 1024, CURRENT_TIMESTAMP)" })
    @Sql(
            statements = {
                    "DELETE FROM events.idempotency_keys",
                    "DELETE FROM events.events_images",
                    "DELETE FROM events.events_categories",
                    "DELETE FROM events.events",
                    "DELETE FROM events.images",
                    "DELETE FROM events.categories" },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void create_whenOneHundredCategoriesAreRequested_shouldPersistAllLinksTest(String scenario, String requestBody, String idempotencyKey)
            throws Exception {
        MvcResult result = mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/api/v1/events")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt()
                                                .jwt(jwt -> jwt.subject("organizer-1"))
                                                .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZER")))
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andReturn();

        Assertions.assertEquals(201, result.getResponse().getStatus());
        Assertions.assertEquals(100, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events.events_categories", Integer.class));
    }

    private MockHttpServletRequestBuilder getWithRole(String path, String role) {
        return MockMvcRequestBuilders.get(path)
                .with(SecurityMockMvcRequestPostProcessors.jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
