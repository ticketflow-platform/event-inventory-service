package scenario.cases.java;

import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public final class EventInventoryServiceApplicationTestCases {

    public static Stream<Arguments> main_whenArgumentsAreProvided_thenLaunchSpringApplicationTestCases() {
        return Stream.of(
            Arguments.of("Аргументы отсутствуют", new String[0]),
            Arguments.of("Параметры переданы приложению", new String[] { "--spring.main.banner-mode=off" })
        );
    }

    private EventInventoryServiceApplicationTestCases() {
    }
}
