package ru.ticketflowplatform.eventinventoryservice.mapper;

import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.validation.FieldError;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto.FieldErrorResponseDto;

class ErrorMapperTest {

    private final ErrorMapper errorMapper = new ErrorMapperImpl();

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource({
        "scenario.cases.java.mapper.ErrorMapperTestCases#toResponse_whenErrorDetailsAreProvided_thenMapErrorResponseTestCases",
        "scenario.cases.java.mapper.ErrorMapperTestCases#toResponse_whenOptionalFieldsAreMissing_thenPreserveNullsTestCases"
    })
    @DisplayName("Преобразует HTTP-ошибку в DTO ответа по сценариям")
    void toResponse_whenErrorDetailsAreProvided_thenMapErrorResponseTest(
        String scenario,
        int status,
        String message,
        String detail,
        List<FieldErrorResponseDto> errors,
        ErrorResponseDto expected) {
        Assertions.assertThat(errorMapper.toResponse(status, message, detail, errors)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.mapper.ErrorMapperTestCases#toFieldError_whenFieldErrorIsProvided_thenMapFieldErrorTestCases")
    @DisplayName("Преобразует ошибку поля по сценариям")
    void toFieldError_whenFieldErrorIsProvided_thenMapFieldErrorTest(
        String scenario,
        FieldError error,
        FieldErrorResponseDto expected) {
        Assertions.assertThat(errorMapper.toFieldError(error)).isEqualTo(expected);
    }
}
