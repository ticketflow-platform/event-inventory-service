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
                        "3908481aa202120104f3e5d0a3f4f4f45744416c66a94abe8cf269a42c83ab07"
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
                        "c493ede7f5a2a2ccd2bc0f09d7b853979ee871a99588f5d37330f8b7534aaa72"
                )
        );
    }
}
