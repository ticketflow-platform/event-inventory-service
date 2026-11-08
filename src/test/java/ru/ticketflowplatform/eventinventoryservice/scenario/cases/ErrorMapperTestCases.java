package ru.ticketflowplatform.eventinventoryservice.scenario.cases;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;

public final class ErrorMapperTestCases {

    public static Stream<Arguments> toResponse_whenErrorDetailsAreProvided_shouldMapErrorResponseTestCases() {
        FieldError fieldError = new FieldError(
                "request", "title", null, false, null, null, "must not be blank"
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
                                List.of(new ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto.FieldErrorResponseDto("title", "must not be blank"))
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

    public static Stream<Arguments> toResponse_whenFieldErrorIsProvided_shouldMapFieldErrorTestCases() {
        return Stream.of(Arguments.of(
                "Ошибка обязательного поля",
                new FieldError("request", "title", null, false, null, null, "must not be blank"),
                new ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto.FieldErrorResponseDto("title", "must not be blank")
        ));
    }
}
