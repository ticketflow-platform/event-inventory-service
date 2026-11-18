package ru.ticketflowplatform.eventinventoryservice.scenario.cases;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto.CategoryRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto.ImageRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.ImageType;

public final class HashServiceTestCases {

    public static Stream<Arguments> hash_whenRequestIsProvided_shouldReturnSha256FingerprintTestCases() {
        return Stream.of(
                Arguments.of(
                        "Описание не задано, обязательные поля заполнены",
                        new CreateEventRequestDto(
                                "Cinema",
                                null,
                                List.of(new CategoryRequestDto(UUID.fromString("00000000-0000-0000-0000-000000000001"))),
                                List.of(new ImageRequestDto(
                                        UUID.fromString("00000000-0000-0000-0000-000000000002"), ImageType.COVER
                                )),
                                0
                        ),
                        "b79fc2c2655b425e47b2a61761ac201dccf453e826c18cdb3875b98904261805"
                ),
                Arguments.of(
                        "Заполнены категории и изображения",
                        new CreateEventRequestDto(
                                "Cinema",
                                "Evening",
                                List.of(new CategoryRequestDto(UUID.fromString("00000000-0000-0000-0000-000000000001"))),
                                List.of(new ImageRequestDto(
                                        UUID.fromString("00000000-0000-0000-0000-000000000002"),
                                        ImageType.COVER
                                )),
                                18
                        ),
                        "8b3543213f766c3945c459cdd9c9c2ff49f53fc277e15b7b1df5ef52a34d1c6d"
                )
        );
    }
}
