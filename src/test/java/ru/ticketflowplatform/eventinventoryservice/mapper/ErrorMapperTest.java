package ru.ticketflowplatform.eventinventoryservice.mapper;

import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto.FieldErrorResponseDto;

class ErrorMapperTest {

    private final ErrorMapper errorMapper = new ErrorMapperImpl();

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.mapper.ErrorMapperTestCases#toResponse_whenErrorDetailsAreProvided_thenMapErrorResponseTestCases")
    @DisplayName("Преобразует HTTP-ошибку в DTO ответа по сценариям")
    void toResponse_whenErrorDetailsAreProvided_thenMapErrorResponseTest(
        String scenario,
        HttpStatus status,
        String detail,
        List<FieldError> errors,
        ErrorResponseDto expected) {
        Assertions.assertThat(
            errorMapper.toResponse(
                status.value(),
                status.getReasonPhrase(),
                detail,
                errors.stream().map(errorMapper::toFieldError).toList()
            )
        ).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.mapper.ErrorMapperTestCases#toResponse_whenOptionalFieldsAreMissing_thenPreserveNullsTestCases")
    @DisplayName("Сохраняет null в необязательных полях результата")
    void toResponse_whenOptionalFieldsAreMissing_thenPreserveNullsTest(
        String scenario,
        String message,
        String detail,
        List<FieldErrorResponseDto> errors,
        ErrorResponseDto expected) {
        Assertions.assertThat(errorMapper.toResponse(400, message, detail, errors)).isEqualTo(expected);
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
