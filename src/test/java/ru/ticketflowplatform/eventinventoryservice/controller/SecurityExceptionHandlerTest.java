package ru.ticketflowplatform.eventinventoryservice.controller;

import helper.TestMessageSources;
import java.nio.charset.StandardCharsets;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;
import tools.jackson.databind.ObjectMapper;

class SecurityExceptionHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecurityExceptionHandler handler = new SecurityExceptionHandler(objectMapper, TestMessageSources.i18n());
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.controller.SecurityExceptionHandlerTestCases#commence_whenAuthenticationFails_thenReturnLocalizedUnauthorizedTestCases")
    @DisplayName("Возвращает локализованную 401-ошибку и Bearer challenge")
    void commence_whenAuthenticationFails_thenReturnLocalizedUnauthorizedTest(
        String scenario,
        MockHttpServletRequest request,
        AuthenticationException exception,
        String expectedChallenge,
        ErrorResponseDto expected) throws Exception {
        handler.commence(request, response, exception);

        Assertions.assertThat(response.getStatus()).isEqualTo(expected.status());
        Assertions.assertThat(response.getHeader("WWW-Authenticate")).isEqualTo(expectedChallenge);
        Assertions.assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        Assertions.assertThat(response.getCharacterEncoding()).isEqualTo(StandardCharsets.UTF_8.name());
        Assertions.assertThat(objectMapper.readValue(response.getContentAsString(), ErrorResponseDto.class)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.controller.SecurityExceptionHandlerTestCases#handle_whenAccessIsDenied_thenReturnLocalizedForbiddenTestCases")
    @DisplayName("Возвращает локализованную 403-ошибку и Bearer challenge")
    void handle_whenAccessIsDenied_thenReturnLocalizedForbiddenTest(
        String scenario,
        MockHttpServletRequest request,
        AccessDeniedException exception,
        String expectedChallenge,
        ErrorResponseDto expected) throws Exception {
        handler.handle(request, response, exception);

        Assertions.assertThat(response.getStatus()).isEqualTo(expected.status());
        Assertions.assertThat(response.getHeader("WWW-Authenticate")).isEqualTo(expectedChallenge);
        Assertions.assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        Assertions.assertThat(response.getCharacterEncoding()).isEqualTo(StandardCharsets.UTF_8.name());
        Assertions.assertThat(objectMapper.readValue(response.getContentAsString(), ErrorResponseDto.class)).isEqualTo(expected);
    }
}
