package scenario.cases.java.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto.CategoryRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto.ImageRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.ImagePosition;

public final class HashServiceTestCases {

    public static Stream<Arguments> hash_whenRequestsHaveDifferentContent_thenReturnDifferentFingerprintsTestCases() {
            return Stream.of(
                Arguments.of(
                    "Разделитель внутри разных полей",
                    new CreateEventRequestDto("a|b", "c", null, null, null),
                    new CreateEventRequestDto("a", "b|c", null, null, null)),
                Arguments.of(
                    "Отсутствующее описание и строка null",
                    new CreateEventRequestDto("Concert", null, null, null, null),
                    new CreateEventRequestDto("Concert", "null", null, null, null)),
                Arguments.of(
                    "Кавычки и обратные слеши не меняют границы полей",
                    new CreateEventRequestDto("a\"|b\\", "c", null, null, null),
                    new CreateEventRequestDto("a\"", "b\\|c", null, null, null)));
    }

    public static Stream<Arguments> hash_whenRequestIsProvided_thenReturnSha256FingerprintTestCases() {
            return Stream.of(
                Arguments.of(
                    "Описание не задано, обязательные поля заполнены",
                    new CreateEventRequestDto(
                        "Cinema",
                        null,
                        List.of(
                            new CategoryRequestDto(
                                UUID.fromString(
                                    "00000000-0000-0000-0000-000000000001"))),
                        List.of(
                            new ImageRequestDto(
                                UUID.fromString(
                                    "00000000-0000-0000-0000-000000000002"),
                                ImagePosition.COVER)),
                        0),
                    "c07d8328562ef0e8e91bbab18e2e80915572689295485e96598a339c78df262a"),
                Arguments.of(
                    "Заполнены категории и изображения",
                    new CreateEventRequestDto(
                        "Cinema",
                        "Evening",
                        List.of(
                            new CategoryRequestDto(
                                UUID.fromString(
                                    "00000000-0000-0000-0000-000000000001"))),
                        List.of(
                            new ImageRequestDto(
                                UUID.fromString(
                                    "00000000-0000-0000-0000-000000000002"),
                                ImagePosition.COVER)),
                        18),
                    "d704f67f20a663cdafb55e9803175e264f88ec190629ecc2d7753908fc792890"));
    }

    public static Stream<Arguments> hash_whenSha256IsUnavailable_thenThrowDomainExceptionTestCases() {
            return Stream.of(
                Arguments.of(
                    "SHA-256 отсутствует в JCA",
                    new CreateEventRequestDto("Cinema", null, null, null, null),
                    new java.security.NoSuchAlgorithmException("SHA-256")));
    }
}
