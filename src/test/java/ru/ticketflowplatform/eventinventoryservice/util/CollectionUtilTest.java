package ru.ticketflowplatform.eventinventoryservice.util;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collector;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class CollectionUtilTest {

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.util.CollectionUtilTestCases#collectOrEmpty_whenCollectorIsProvided_thenCollectResultTestCases")
    @DisplayName("Собирает результат по правилам Collector, считая null пустым источником")
    <R> void collectOrEmpty_whenCollectorIsProvided_thenCollectResultTest(
        String scenario, Collection<String> source, Collector<String, ?, R> collector,
        R expected, Class<?> expectedType) {
            R actual = CollectionUtil.collectOrEmpty(source, collector);
            Assertions.assertThat(actual).isInstanceOf(expectedType).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.util.CollectionUtilTestCases#collectOrEmpty_whenMapKeysAreDuplicated_thenPropagateCollectorErrorTestCases")
    @DisplayName("Не скрывает ошибку Collector при повторяющихся ключах")
    <R> void collectOrEmpty_whenMapKeysAreDuplicated_thenPropagateCollectorErrorTest(
        String scenario, Collection<String> source, Collector<String, ?, R> collector, String expectedMessage) {
            Assertions.assertThatThrownBy(() -> CollectionUtil.collectOrEmpty(source, collector))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(expectedMessage);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.util.CollectionUtilTestCases#filterDistinctOrEmpty_whenRuleIsProvided_thenFilterAndCollectTestCases")
    @DisplayName("Фильтрует по Predicate, убирает повторы и собирает результат через Collector")
    <R> void filterDistinctOrEmpty_whenRuleIsProvided_thenFilterAndCollectTest(
        String scenario, Collection<String> source, Predicate<String> rule, Collector<String, ?, R> collector,
        R expected, Class<?> expectedType) {
            R actual = CollectionUtil.filterDistinctOrEmpty(source, rule, collector);
            Assertions.assertThat(actual).isInstanceOf(expectedType).isEqualTo(expected);
    }
}
