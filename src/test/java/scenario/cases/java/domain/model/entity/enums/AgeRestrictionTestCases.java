package scenario.cases.java.domain.model.entity.enums;

import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public final class AgeRestrictionTestCases {

    public static Stream<Arguments> isSupport_whenValueIsChecked_thenReturnSupportStatusTestCases() {
        return Stream.of(
            Arguments.of(0, true),
            Arguments.of(6, true),
            Arguments.of(12, true),
            Arguments.of(16, true),
            Arguments.of(18, true),
            Arguments.of(null, false),
            Arguments.of(-1, false),
            Arguments.of(21, false));
    }
}
