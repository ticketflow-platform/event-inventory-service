package ru.ticketflowplatform.eventinventoryservice.controller;

import helper.BaseIntegrationTest;
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
import ru.ticketflowplatform.eventinventoryservice.domain.exception.EventNotFoundException;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;
import ru.ticketflowplatform.eventinventoryservice.service.EventService;

@Sql(scripts = "/scenario/cases/sql/clear-event-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/scenario/cases/sql/clear-event-data.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class EventNotFoundApiTest extends BaseIntegrationTest {

    @MockitoBean
    private EventService eventService;

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.controller.EventNotFoundApiTestCases#create_whenStoredEventIsMissing_thenReturnNotFoundTestCases")
    @DisplayName("Возвращает полную 404-ошибку, если событие из ключа отсутствует")
    void create_whenStoredEventIsMissing_thenReturnNotFoundTest(
        String scenario,
        String requestBody,
        String idempotencyKey,
        EventNotFoundException failure,
        ErrorResponseDto expectedError) throws Exception {
        Mockito.when(eventService.create(
            Mockito.any(CreateEventRequestDto.class),
            Mockito.eq("organizer-1"),
            Mockito.eq(idempotencyKey)
        )).thenThrow(failure);

        MvcResult result = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/events")
                .with(
                    SecurityMockMvcRequestPostProcessors.jwt()
                        .jwt(jwt -> jwt.subject("organizer-1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZER"))
                )
                .header("Idempotency-Key", idempotencyKey)
                .header("Accept-Language", "en")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        ).andReturn();
        ErrorResponseDto error = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponseDto.class);

        Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(expectedError.status());
        Assertions.assertThat(error).isEqualTo(expectedError);
    }
}
