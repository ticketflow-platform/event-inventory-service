package scenario.cases.java.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public final class CollectionUtilTestCases {

    public static Stream<Arguments> filterDistinctOrEmpty_whenRuleIsProvided_thenFilterAndCollectTestCases() {
        Predicate<String> longerThanThree = value -> value.length() > 3;
        Set<String> seen = new HashSet<>();
        Predicate<String> repeated = value -> !seen.add(value);
        return Stream.of(
            Arguments.of("Null собирается в пустой список", null, longerThanThree,
                Collectors.toList(), List.of(), List.class),
            Arguments.of("Пустой источник", List.of(), longerThanThree,
                Collectors.toList(), List.of(), List.class),
            Arguments.of("Отбор, порядок и удаление повторов", List.of("event", "api", "show", "event"),
                longerThanThree,
                Collectors.toList(), List.of("event", "show"), List.class),
            Arguments.of("Нет совпадений", List.of("api", "web"), longerThanThree,
                Collectors.toList(), List.of(), List.class),
            Arguments.of("Результат может быть ArrayList", Set.of("event"), longerThanThree,
                Collectors.toCollection(ArrayList::new), List.of("event"), ArrayList.class),
            Arguments.of("Результат может быть Set", List.of("api", "event", "event"), longerThanThree,
                Collectors.toSet(), Set.of("event"), Set.class),
            Arguments.of("Результат может быть Map с преобразованием", List.of("api", "event", "event"),
                longerThanThree,
                Collectors.toMap(String::length, Function.identity()), Map.of(5, "event"), Map.class),
            Arguments.of("Фильтр выполняется до distinct", List.of("web", "api", "web", "web", "api"), repeated,
                Collectors.toList(), List.of("web", "api"), List.class),
            Arguments.of("Finisher выполняется и для null", null, longerThanThree,
                Collectors.joining(",", "[", "]"), "[]", String.class));
    }

    public static Stream<Arguments> collectOrEmpty_whenCollectorIsProvided_thenCollectResultTestCases() {
        Collector<String, ?, ArrayList<Integer>> lengths = Collectors.mapping(
            String::length, Collectors.toCollection(ArrayList::new));
        Collector<String, ?, Map<Integer, String>> byLength = Collectors.toMap(String::length, Function.identity());
        return Stream.of(
            Arguments.of("Null собирается в ArrayList", null, lengths, List.of(), ArrayList.class),
            Arguments.of("Пустой список", List.of(), lengths, List.of(), ArrayList.class),
            Arguments.of("Порядок и повторения сохраняются", List.of("event", "api", "event"), lengths,
                List.of(5, 3, 5), ArrayList.class),
            Arguments.of("Исходная коллекция может быть множеством", Set.of("api"), lengths,
                List.of(3), ArrayList.class),
            Arguments.of("Null собирается в Map", null, byLength, Map.of(), Map.class),
            Arguments.of("Ключ и значение определяет Collector", List.of("event", "api"), byLength,
                Map.of(5, "event", 3, "api"), Map.class),
            Arguments.of("Уникальные длины", List.of("api", "web", "event"),
                Collectors.mapping(String::length, Collectors.toSet()), Set.of(3, 5), Set.class),
            Arguments.of("Null собирается в Set", null, Collectors.toSet(), Set.of(), Set.class),
            Arguments.of("Группировка по длине", List.of("api", "event", "web"),
                Collectors.groupingBy(String::length), Map.of(3, List.of("api", "web"), 5, List.of("event")),
                Map.class),
            Arguments.of("Явное правило объединения повторяющихся ключей", List.of("api", "web"),
                Collectors.toMap(String::length, Function.identity(), (first, second) -> first + "," + second),
                Map.of(3, "api,web"), Map.class),
            Arguments.of("Collector может возвращать не коллекцию", List.of("api", "event"),
                Collectors.joining(","), "api,event", String.class),
            Arguments.of("Для null выполняется finisher Collector", null,
                Collectors.joining(",", "[", "]"), "[]", String.class));
    }

    public static Stream<Arguments> collectOrEmpty_whenMapKeysAreDuplicated_thenPropagateCollectorErrorTestCases() {
        return Stream.of(Arguments.of(
            "Повторение ключа без merge-функции",
            List.of("api", "web"),
            Collectors.toMap(String::length, Function.identity()),
            "Duplicate key 3 (attempted merging values api and web)"));
    }

    private CollectionUtilTestCases() {
    }
}
