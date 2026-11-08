package ru.ticketflowplatform.eventinventoryservice.scenario.cases;

import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public final class AgeRestrictionTestCases {

    public static Stream<Arguments> isSupport_whenValueIsChecked_shouldReturnSupportStatusTestCases() {
        return Stream.of(
                Arguments.of(0, true),
                Arguments.of(6, true),
                Arguments.of(12, true),
                Arguments.of(16, true),
                Arguments.of(18, true),
                Arguments.of(null, false),
                Arguments.of(-1, false),
                Arguments.of(21, false)
        );
    }
}
