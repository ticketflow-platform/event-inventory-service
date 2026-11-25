package scenario.cases.java.controller;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto.FieldErrorResponseDto;

public final class ValidationExceptionApiTestCases {

    public static Stream<Arguments> get_whenQueryParameterIsBlank_thenReturnBadRequestTestCases() {
        return Stream.of(
            Arguments.of(
                "Именованный параметр запроса",
                "/__test/validation/query",
                "probe",
                " ",
                queryError("probe")
            ),
            Arguments.of(
                "Неявно именованный параметр запроса",
                "/__test/validation/query/implicit",
                "probe",
                " ",
                queryError("probe")
            )
        );
    }

    public static Stream<Arguments> get_whenHeaderIsBlank_thenReturnBadRequestTestCases() {
        return Stream.of(
            Arguments.of(
                "Неявно именованный заголовок",
                "/__test/validation/header",
                "probe",
                " ",
                queryError("probe")
            )
        );
    }

    public static Stream<Arguments> post_whenBodyFieldIsBlank_thenReturnBadRequestTestCases() {
        return Stream.of(
            Arguments.of(
                "Пустое поле probe",
                "/__test/validation/body",
                "{\"probe\":\" \"}",
                new ErrorResponseDto(
                    400,
                    "Некорректный запрос",
                    "Проверка запроса не пройдена",
                    List.of(new FieldErrorResponseDto("probe", "Probe must not be blank"))
                )
            )
        );
    }

    private static ErrorResponseDto queryError(String field) {
        return new ErrorResponseDto(
            400,
            "Некорректный запрос",
            "Проверка запроса не пройдена",
            List.of(new FieldErrorResponseDto(field, "Probe must not be blank"))
        );
    }

    private ValidationExceptionApiTestCases() {
    }
}
