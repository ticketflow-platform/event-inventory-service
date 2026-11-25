package ru.ticketflowplatform.eventinventoryservice.controller;

import helper.BaseIntegrationTest;
import helper.HandlerValidationProbeController;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;

@Import(HandlerValidationProbeController.class)
class ValidationExceptionApiTest extends BaseIntegrationTest {

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.controller.ValidationExceptionApiTestCases#get_whenQueryParameterIsBlank_thenReturnBadRequestTestCases")
    @DisplayName("Возвращает полную ошибку для пустого параметра запроса")
    void get_whenQueryParameterIsBlank_thenReturnBadRequestTest(
        String scenario,
        String path,
        String parameterName,
        String parameterValue,
        ErrorResponseDto expectedError) throws Exception {
        MvcResult result = mockMvc.perform(
            MockMvcRequestBuilders.get(path)
                .with(SecurityMockMvcRequestPostProcessors.jwt())
                .param(parameterName, parameterValue)
        ).andReturn();
        ErrorResponseDto error = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponseDto.class);

        Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(expectedError.status());
        Assertions.assertThat(error).isEqualTo(expectedError);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.controller.ValidationExceptionApiTestCases#get_whenHeaderIsBlank_thenReturnBadRequestTestCases")
    @DisplayName("Возвращает полную ошибку для пустого заголовка")
    void get_whenHeaderIsBlank_thenReturnBadRequestTest(
        String scenario,
        String path,
        String headerName,
        String headerValue,
        ErrorResponseDto expectedError) throws Exception {
        MvcResult result = mockMvc.perform(
            MockMvcRequestBuilders.get(path)
                .with(SecurityMockMvcRequestPostProcessors.jwt())
                .header(headerName, headerValue)
        ).andReturn();
        ErrorResponseDto error = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponseDto.class);

        Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(expectedError.status());
        Assertions.assertThat(error).isEqualTo(expectedError);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.controller.ValidationExceptionApiTestCases#post_whenBodyFieldIsBlank_thenReturnBadRequestTestCases")
    @DisplayName("Возвращает полную ошибку для невалидного поля тела")
    void post_whenBodyFieldIsBlank_thenReturnBadRequestTest(
        String scenario,
        String path,
        String requestBody,
        ErrorResponseDto expectedError) throws Exception {
        MvcResult result = mockMvc.perform(
            MockMvcRequestBuilders.post(path)
                .with(SecurityMockMvcRequestPostProcessors.jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        ).andReturn();
        ErrorResponseDto error = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponseDto.class);

        Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(expectedError.status());
        Assertions.assertThat(error).isEqualTo(expectedError);
    }
}
