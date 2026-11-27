package scenario.cases.java.mapper;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.validation.FieldError;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto.FieldErrorResponseDto;

public final class ErrorMapperTestCases {

    public static Stream<Arguments> toResponse_whenErrorDetailsAreProvided_thenMapErrorResponseTestCases() {
            return Stream.of(
                Arguments.of(
                    "Ошибка валидации с полем",
                    400,
                    "Bad Request",
                    "Request validation failed",
                    List.of(new FieldErrorResponseDto("title", "must not be blank")),
                    new ErrorResponseDto(
                        400,
                        "Bad Request",
                        "Request validation failed",
                        List.of(new FieldErrorResponseDto("title", "must not be blank")))),
                Arguments.of(
                    "Ошибка без полей",
                    409,
                    "Conflict",
                    "Key conflict",
                    List.of(),
                    new ErrorResponseDto(409, "Conflict", "Key conflict", List.of())));
    }

    public static Stream<Arguments> toFieldError_whenFieldErrorIsProvided_thenMapFieldErrorTestCases() {
            return Stream.of(
                Arguments.of(
                    "Ошибка обязательного поля",
                    new FieldError(
                        "request", "title", null, false, null, null, "must not be blank"),
                    new FieldErrorResponseDto("title", "must not be blank")),
                Arguments.of("Ошибка поля отсутствует", null, null));
    }

    public static Stream<Arguments> toResponse_whenOptionalFieldsAreMissing_thenPreserveNullsTestCases() {
            return Stream.of(
                Arguments.of("Все nullable-поля отсутствуют", 400, null, null, null, null),
                Arguments.of(
                    "Только detail задан",
                    400,
                    null,
                    "Only detail",
                    null,
                    new ErrorResponseDto(400, null, "Only detail", null)),
                Arguments.of(
                    "Только список ошибок задан",
                    400,
                    null,
                    null,
                    List.of(),
                    new ErrorResponseDto(400, null, null, List.of())),
                Arguments.of(
                    "Список ошибок отсутствует",
                    400,
                    "Bad request",
                    null,
                    null,
                    new ErrorResponseDto(400, "Bad request", null, null)));
    }
}
