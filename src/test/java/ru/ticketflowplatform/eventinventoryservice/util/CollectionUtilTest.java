package ru.ticketflowplatform.eventinventoryservice.util;

import java.util.ArrayList;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.DisplayName;

class CollectionUtilTest {

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.util.CollectionUtilTestCases#mapOrEmpty_whenSourceIsProvided_thenMapOrCreateEmptyCollectionTestCases")
    @DisplayName("Преобразует коллекцию или возвращает пустую")
    void mapOrEmpty_whenSourceIsProvided_thenMapOrCreateEmptyCollectionTest(
        String scenario,
        List<String> source,
        List<Integer> expected) {
        List<Integer> actual = CollectionUtil.mapOrEmpty(source, String::length, ArrayList::new);
        Assertions.assertThat(actual).isEqualTo(expected);
    }
}
