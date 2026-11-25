package ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums;

import org.junit.jupiter.api.DisplayName;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class AgeRestrictionTest {

    @ParameterizedTest(name = "{index}: значение {0}")
    @MethodSource("scenario.cases.java.domain.model.entity.enums.AgeRestrictionTestCases#isSupport_whenValueIsChecked_thenReturnSupportStatusTestCases")
    @DisplayName("Проверяет поддержку возрастного ограничения по сценариям")
    void isSupport_whenValueIsChecked_thenReturnSupportStatusTest(Integer value, boolean expected) {
        Assertions.assertThat(AgeRestriction.isSupport(value)).isEqualTo(expected);
    }
}
