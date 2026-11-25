package ru.ticketflowplatform.eventinventoryservice.controller;

import helper.ExceptionHandlerTestHelper;
import java.nio.charset.StandardCharsets;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;
import tools.jackson.databind.ObjectMapper;

class GlobalExceptionHandlerTest {

    private final HandlerExceptionResolver resolver = ExceptionHandlerTestHelper.globalResolver();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource({
        "scenario.cases.java.controller.GlobalExceptionHandlerTestCases#resolveException_whenRequestFails_thenReturnLocalizedErrorTestCases",
        "scenario.cases.java.controller.GlobalExceptionHandlerTestCases#resolveException_whenMethodValidationFails_thenIdentifyInvalidInputTestCases"
    })
    @DisplayName("Преобразует исключения в полный локализованный HTTP-ответ")
    void resolveException_whenRequestFails_thenReturnLocalizedErrorTest(
        String scenario,
        MockHttpServletRequest request,
        Exception exception,
        ErrorResponseDto expected) throws Exception {
        ModelAndView result = resolver.resolveException(request, response, null, exception);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.isEmpty()).isTrue();
        Assertions.assertThat(response.getStatus()).isEqualTo(expected.status());
        Assertions.assertThat(MediaType.parseMediaType(response.getContentType())).isEqualTo(MediaType.APPLICATION_JSON);
        Assertions.assertThat(objectMapper.readValue(
            response.getContentAsString(StandardCharsets.UTF_8), ErrorResponseDto.class
        )).isEqualTo(expected);
    }
}
