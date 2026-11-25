package scenario.cases.java.controller;

import helper.ExceptionHandlerTestHelper;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;

public final class SecurityExceptionHandlerTestCases {

    private SecurityExceptionHandlerTestCases() {
    }

    public static Stream<Arguments> commence_whenAuthenticationFails_thenReturnLocalizedUnauthorizedTestCases() {
        Locale ru = Locale.forLanguageTag("ru");
        String challenge = "Bearer resource_metadata=\"http://localhost/.well-known/oauth-protected-resource\"";
        JwtValidationException invalidAudience = new JwtValidationException("Validation failed", List.of(
            new OAuth2Error("invalid_token", "Invalid audience", null)
        ));
        return Stream.of(
            Arguments.of(
                "Некорректный bearer-токен и OAuth2 challenge", ExceptionHandlerTestHelper.request(Locale.ENGLISH),
                new InvalidBearerTokenException("Token expired"),
                "Bearer error=\"invalid_token\", error_description=\"Token expired\", error_uri=\"https://tools.ietf.org/html/rfc6750#section-3.1\", resource_metadata=\"http://localhost/.well-known/oauth-protected-resource\"",
                new ErrorResponseDto(401, "Authentication required", "A valid access token is required to access this resource", List.of())
            ),
            Arguments.of(
                "Нет корректных credentials: русский", ExceptionHandlerTestHelper.request(ru),
                new BadCredentialsException("Invalid credentials"), challenge,
                new ErrorResponseDto(401, "Требуется аутентификация", "Для доступа к ресурсу необходим действительный токен", List.of())
            ),
            Arguments.of(
                "Нет корректных credentials: английский", ExceptionHandlerTestHelper.request(Locale.ENGLISH),
                new BadCredentialsException("Invalid credentials"), challenge,
                new ErrorResponseDto(401, "Authentication required", "A valid access token is required to access this resource", List.of())
            ),
            Arguments.of(
                "Неверная аудитория: русский", ExceptionHandlerTestHelper.request(ru),
                new BadCredentialsException("JWT rejected", invalidAudience), challenge,
                new ErrorResponseDto(401, "Токен предназначен для другого сервиса", "Аудитория токена не соответствует этому API", List.of())
            ),
            Arguments.of(
                "Аудитория найдена во вложенной причине: английский", ExceptionHandlerTestHelper.request(Locale.ENGLISH),
                new BadCredentialsException("JWT rejected", new IllegalStateException("Decoder failed", invalidAudience)), challenge,
                new ErrorResponseDto(401, "Token audience mismatch", "This access token is not intended for this API", List.of())
            ),
            Arguments.of(
                "Другая ошибка валидации JWT", ExceptionHandlerTestHelper.request(Locale.ENGLISH),
                new BadCredentialsException("JWT rejected", new JwtValidationException("Expired", List.of(
                    new OAuth2Error("invalid_token", "Jwt expired", null)
                ))), challenge,
                new ErrorResponseDto(401, "Authentication required", "A valid access token is required to access this resource", List.of())
            ),
            Arguments.of(
                "Описание ошибки JWT отсутствует", ExceptionHandlerTestHelper.request(Locale.ENGLISH),
                new BadCredentialsException("JWT rejected", new JwtValidationException("Invalid JWT", List.of(
                    new OAuth2Error("invalid_token")
                ))), challenge,
                new ErrorResponseDto(401, "Authentication required", "A valid access token is required to access this resource", List.of())
            )
        );
    }

    public static Stream<Arguments> handle_whenAccessIsDenied_thenReturnLocalizedForbiddenTestCases() {
        MockHttpServletRequest authenticatedRequest = ExceptionHandlerTestHelper.request(Locale.ENGLISH);
        authenticatedRequest.setUserPrincipal(new JwtAuthenticationToken(
            Jwt.withTokenValue("token").header("alg", "RS256").claim("sub", "organizer-1").build()
        ));
        return Stream.of(
            Arguments.of(
                "JWT не даёт достаточных полномочий", authenticatedRequest,
                new AccessDeniedException("Denied"),
                "Bearer error=\"insufficient_scope\", error_description=\"The request requires higher privileges than provided by the access token.\", error_uri=\"https://tools.ietf.org/html/rfc6750#section-3.1\"",
                new ErrorResponseDto(403, "Access denied", "You do not have permission to perform this operation", List.of())
            ),
            Arguments.of(
                "Недостаточно прав: русский", ExceptionHandlerTestHelper.request(Locale.forLanguageTag("ru")),
                new AccessDeniedException("Denied"), "Bearer",
                new ErrorResponseDto(403, "Доступ запрещён", "Недостаточно прав для выполнения операции", List.of())
            ),
            Arguments.of(
                "Недостаточно прав: английский", ExceptionHandlerTestHelper.request(Locale.ENGLISH),
                new AccessDeniedException("Denied"), "Bearer",
                new ErrorResponseDto(403, "Access denied", "You do not have permission to perform this operation", List.of())
            )
        );
    }
}
