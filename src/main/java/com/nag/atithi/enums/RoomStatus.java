package com.nag.atithi.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RoomStatus {
    VACANT("Vacant"),
    OCCUPIED("Occupied"),
    STAY_OVER("Stay Over"),
    CHECK_IN("Check In"),
    CHECK_OUT("Check Out");

    private final String value;

    RoomStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static RoomStatus fromValue(String value) {
        if (value == null) return null;
        for (RoomStatus status : RoomStatus.values()) {
            if (status.value.equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown room status: " + value);
    }
}
