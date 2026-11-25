package scenario.cases.java.util;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public final class CollectionUtilTestCases {

    public static Stream<Arguments> mapOrEmpty_whenSourceIsProvided_thenMapOrCreateEmptyCollectionTestCases() {
        return Stream.of(
            Arguments.of("Null превращается в пустой список", null, List.of()),
            Arguments.of("Значения преобразуются", List.of("event", "api"), List.of(5, 3))
        );
    }
    private CollectionUtilTestCases() {
    }
}
