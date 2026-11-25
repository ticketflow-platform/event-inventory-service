package scenario.cases.java.mapper;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;

public final class ErrorMapperTestCases {

    public static Stream<Arguments> toResponse_whenErrorDetailsAreProvided_thenMapErrorResponseTestCases() {
        FieldError fieldError = new FieldError(
            "request",
            "title",
            null,
            false,
            null,
            null,
            "must not be blank"
        );
        return Stream.of(
            Arguments.of(
                "Ошибка валидации с полем",
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                List.of(fieldError),
                new ErrorResponseDto(
                    400,
                    "Bad Request",
                    "Request validation failed",
                    List.of(
                        new ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto.FieldErrorResponseDto(
                            "title",
                            "must not be blank"
                        )
                    )
                )
            ),
            Arguments.of(
                "Ошибка без полей",
                HttpStatus.CONFLICT,
                "Key conflict",
                List.of(),
                new ErrorResponseDto(409, "Conflict", "Key conflict", List.of())
            )
        );
    }

    public static Stream<Arguments> toFieldError_whenFieldErrorIsProvided_thenMapFieldErrorTestCases() {
        return Stream.of(
            Arguments.of(
                "Ошибка обязательного поля",
                new FieldError("request", "title", null, false, null, null, "must not be blank"),
                new ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto.FieldErrorResponseDto(
                    "title",
                    "must not be blank"
                )
            ),
            Arguments.of("Ошибка поля отсутствует", null, null)
        );
    }

    public static Stream<Arguments> toResponse_whenOptionalFieldsAreMissing_thenPreserveNullsTestCases() {
        return Stream.of(
            Arguments.of("Все nullable-поля отсутствуют", null, null, null, null),
            Arguments.of(
                "Только detail задан",
                null,
                "Only detail",
                null,
                new ErrorResponseDto(400, null, "Only detail", null)
            ),
            Arguments.of(
                "Только список ошибок задан",
                null,
                null,
                List.of(),
                new ErrorResponseDto(400, null, null, List.of())
            ),
            Arguments.of(
                "Список ошибок отсутствует",
                "Bad request",
                null,
                null,
                new ErrorResponseDto(400, "Bad request", null, null)
            )
        );
    }
}
