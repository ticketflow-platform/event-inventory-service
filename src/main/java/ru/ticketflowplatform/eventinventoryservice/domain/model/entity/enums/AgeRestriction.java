package ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums;

import java.util.Arrays;
import lombok.Getter;

@Getter
public enum AgeRestriction {
    NONE(0),
    SIX(6),
    TWELVE(12),
    SIXTEEN(16),
    EIGHTEEN(18);

    private final int value;

    AgeRestriction(int value) {
        this.value = value;
    }

    public static boolean isSupport(Integer value) {
        return value != null && Arrays.stream(values()).anyMatch(restriction -> restriction.value == value);
    }
}
