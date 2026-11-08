package ru.ticketflowplatform.eventinventoryservice.mapper;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;

class ErrorMapperTest {

    private final ErrorMapper errorMapper = new ErrorMapperImpl();

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.ErrorMapperTestCases#toResponse_whenErrorDetailsAreProvided_shouldMapErrorResponseTestCases")
    @DisplayName("Преобразует HTTP-ошибку в DTO ответа по сценариям")
    void toResponse_whenErrorDetailsAreProvided_shouldMapErrorResponseTest(
            String scenario,
            HttpStatus status,
            String detail,
            List<FieldError> errors,
            ErrorResponseDto expected
    ) {
        Assertions.assertEquals(expected, errorMapper.toResponse(
                status.value(), status.getReasonPhrase(), detail,
                errors.stream().map(errorMapper::toFieldError).toList()
        ));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.ErrorMapperTestCases#toResponse_whenFieldErrorIsProvided_shouldMapFieldErrorTestCases")
    @DisplayName("Преобразует ошибку поля по сценариям")
    void toFieldError_whenFieldErrorIsProvided_shouldMapFieldErrorTest(
            String scenario,
            FieldError error,
            ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto.FieldErrorResponseDto expected
    ) {
        Assertions.assertEquals(expected, errorMapper.toFieldError(error));
    }
}
