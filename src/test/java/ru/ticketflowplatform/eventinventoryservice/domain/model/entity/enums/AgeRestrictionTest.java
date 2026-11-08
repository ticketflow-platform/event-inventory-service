package ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class AgeRestrictionTest {

    @ParameterizedTest(name = "{index}: значение {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.AgeRestrictionTestCases#isSupport_whenValueIsChecked_shouldReturnSupportStatusTestCases")
    @DisplayName("Проверяет поддержку возрастного ограничения по сценариям")
    void isSupport_whenValueIsChecked_shouldReturnSupportStatusTest(Integer value, boolean expected) {
        Assertions.assertEquals(expected, AgeRestriction.isSupport(value));
    }
}
