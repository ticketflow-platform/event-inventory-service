package scenario.cases.java.controller;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.HashGenerationException;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;

public final class HashGenerationApiTestCases {

    public static Stream<Arguments> create_whenRequestFingerprintCannotBeGenerated_thenReturnInternalErrorTestCases() {
        NoSuchAlgorithmException cause = new NoSuchAlgorithmException("SHA-256");
        return Stream.of(
            Arguments.of(
                "SHA-256 недоступен",
                "{\"title\":\"Festival\"}",
                "hash-failure",
                new HashGenerationException(cause),
                cause,
                new ErrorResponseDto(
                    500,
                    "Ошибка обработки запроса",
                    "Не удалось сформировать контрольную сумму запроса",
                    List.of()
                )
            )
        );
    }

    private HashGenerationApiTestCases() {
    }
}
