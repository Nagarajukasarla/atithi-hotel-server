package com.nag.atithi.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PremiumType {
    FEMALE("FEMALE"),
    MALE("MALE");

    private final String value;

    PremiumType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PremiumType fromValue(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        for (PremiumType type : PremiumType.values()) {
            if (type.value.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown premium option type: " + value);
    }
}
