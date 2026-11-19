package ru.ticketflowplatform.eventinventoryservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

class SecurityExceptionHandlerTest {

    @Test
    void invalidAudience_shouldReturnLocalizedUnauthorizedError() throws Exception {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        SecurityExceptionHandler handler = new SecurityExceptionHandler(new ObjectMapper(), messageSource);
        JwtValidationException validationException = new JwtValidationException(
                "Invalid JWT",
                List.of(new OAuth2Error(
                        "invalid_token",
                        messageSource.getMessage(
                                "security.jwt.invalid_audience.error_description",
                                new Object[0],
                                Locale.ENGLISH),
                        null)));
        InvalidBearerTokenException exception = new InvalidBearerTokenException("Invalid bearer token", validationException);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addPreferredLocale(Locale.forLanguageTag("ru"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.commence(request, response, exception);

        JsonNode error = new ObjectMapper().readTree(response.getContentAsString());
        Assertions.assertEquals(401, response.getStatus());
        Assertions.assertEquals("Токен предназначен для другого сервиса", error.path("message").asText());
        Assertions.assertEquals("Аудитория токена не соответствует этому API", error.path("detail").asText());
    }
}
