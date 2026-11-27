package ru.ticketflowplatform.eventinventoryservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception)
        throws IOException {
            new BearerTokenAuthenticationEntryPoint().commence(request, response, exception);
            boolean invalidAudience = hasInvalidAudience(exception);
            writeError(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                invalidAudience
                    ? "api.error.invalid_audience.message"
                    : "api.error.unauthorized.message",
                invalidAudience
                    ? "api.error.invalid_audience.detail"
                    : "api.error.unauthorized.detail");
    }

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException exception)
        throws IOException {
            new BearerTokenAccessDeniedHandler().handle(request, response, exception);
            writeError(
                request,
                response,
                HttpStatus.FORBIDDEN,
                "api.error.forbidden.message",
                "api.error.forbidden.detail");
    }

    private void writeError(
        HttpServletRequest request,
        HttpServletResponse response,
        HttpStatus status,
        String messageCode,
        String detailCode)
        throws IOException {
            response.setStatus(status.value());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(
                response.getOutputStream(),
                new ErrorResponseDto(
                    status.value(),
                    messageSource.getMessage(messageCode, null, request.getLocale()),
                    messageSource.getMessage(detailCode, null, request.getLocale()),
                    List.of()));
    }

    private boolean hasInvalidAudience(Throwable exception) {
            String invalidAudienceDescription = messageSource.getMessage(
                "security.jwt.invalid_audience.error_description",
                null,
                Locale.ENGLISH);

            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof JwtValidationException validationException
                    && validationException.getErrors().stream()
                        .anyMatch(error -> invalidAudienceDescription.equals(error.getDescription()))) {
                    return true;
                }
            }

            return false;
    }
}
