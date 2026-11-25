package scenario.cases.java.config;

import helper.SecurityConfigHelper;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;

public final class SecurityConfigApiTestCases {

    public static Stream<Arguments> post_whenOrganizerJwtIsValid_thenCreateDraftTestCases() {
        return Stream.of(
            Arguments.of(
                "Роль организатора из realm_access",
                SecurityConfigHelper.signedToken(
                    "http://localhost:8080/realms/ticketflow",
                    List.of("ticketflow"),
                    Map.of("roles", List.of("ORGANIZER"))
                ),
                "{\"title\":\"Security test\"}",
                "jwt-organizer",
                "security-test-organizer",
                "Security test"
            )
        );
    }

    public static Stream<Arguments> post_whenJwtDoesNotAuthorizeRequest_thenReturnCompleteErrorTestCases() {
        String issuer = "http://localhost:8080/realms/ticketflow";
        String requestBody = "{\"title\":\"Security test\"}";
        return Stream.of(
            Arguments.of(
                "realm_access отсутствует",
                SecurityConfigHelper.signedToken(issuer, List.of("ticketflow"), null),
                requestBody,
                "jwt-no-realm-access",
                new ErrorResponseDto(403, "Access denied", "You do not have permission to perform this operation", List.of())
            ),
            Arguments.of(
                "Пользователь без ролей",
                SecurityConfigHelper.signedToken(issuer, List.of("ticketflow"), Map.of()),
                requestBody,
                "jwt-no-role",
                new ErrorResponseDto(403, "Access denied", "You do not have permission to perform this operation", List.of())
            ),
            Arguments.of(
                "realm_access содержит некорректный список",
                SecurityConfigHelper.signedToken(issuer, List.of("ticketflow"), Map.of("roles", "ORGANIZER")),
                requestBody,
                "jwt-invalid-roles",
                new ErrorResponseDto(403, "Access denied", "You do not have permission to perform this operation", List.of())
            ),
            Arguments.of(
                "Пустой список ролей",
                SecurityConfigHelper.signedToken(issuer, List.of("ticketflow"), Map.of("roles", List.of())),
                requestBody,
                "jwt-empty-roles",
                new ErrorResponseDto(403, "Access denied", "You do not have permission to perform this operation", List.of())
            ),
            Arguments.of(
                "Неверная аудитория",
                SecurityConfigHelper.signedToken(issuer, List.of("other-service"), Map.of("roles", List.of("ORGANIZER"))),
                requestBody,
                "jwt-invalid-audience",
                new ErrorResponseDto(
                    401,
                    "Token audience mismatch",
                    "This access token is not intended for this API",
                    List.of()
                )
            ),
            Arguments.of(
                "Неверный издатель",
                SecurityConfigHelper.signedToken(
                    "https://invalid.example/issuer",
                    List.of("ticketflow"),
                    Map.of("roles", List.of("ORGANIZER"))
                ),
                requestBody,
                "jwt-invalid-issuer",
                new ErrorResponseDto(
                    401,
                    "Authentication required",
                    "A valid access token is required to access this resource",
                    List.of()
                )
            )
        );
    }

    private SecurityConfigApiTestCases() {
    }
}
