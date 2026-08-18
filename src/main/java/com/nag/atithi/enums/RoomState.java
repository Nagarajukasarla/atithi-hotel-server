package com.nag.atithi.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RoomState {
    CLEAN("Clean"),
    DIRTY("Dirty"),
    INSPECTED("Inspected"),
    OUT_OF_SERVICE("Out of Service"),
    OUT_OF_ORDER("Out of Order");

    private final String value;

    RoomState(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static RoomState fromValue(String value) {
        if (value == null) return null;
        for (RoomState state : RoomState.values()) {
            if (state.value.equalsIgnoreCase(value.trim())) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown room state: " + value);
    }
}
