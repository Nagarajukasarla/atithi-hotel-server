# Room Type Premium Configurations Backend Implementation Plan

This guide details the backend changes required in the `atithi-server` application to support the new frontend `isPremium` toggle and `premiumType` (FEMALE/MALE) configurations on Room Types.

---

## 1. Database Migrations / Schema Changes

Apply the following DDL migration to add `is_premium` and `premium_type` columns to the `room_types` table.

```sql
-- migration file V3__Add_Premium_Fields_To_Room_Types.sql
ALTER TABLE room_types 
ADD COLUMN is_premium BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN premium_type VARCHAR(20) DEFAULT NULL;
```

---

## 2. Java Layer Implementations

Follow the Spring Boot structure in package `com.nag.atithi`:

### A. Premium Option Type Enum (`com.nag.atithi.enums.PremiumType`)
Define the options corresponding to the frontend choices:

```java
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
```

### B. Entity Updates (`com.nag.atithi.models.RoomType`)
In the existing `RoomType` entity, append the new properties with JPA mapping:

```java
    // ... Existing RoomType fields ...

    @Column(name = "is_premium", nullable = false)
    @Builder.Default
    private Boolean isPremium = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "premium_type", length = 20)
    private PremiumType premiumType;
```

---

## 3. Requests & DTO Updates

### A. Request Payloads (`com.nag.atithi.requests.RoomTypeRequest`)
Add fields to request payloads used by POST/PUT endpoints for Room Types:

```java
    // ... Existing RoomType fields ...

    private Boolean isPremium;
    private PremiumType premiumType;
```

### B. Response Wrapper (`com.nag.atithi.responses.RoomTypeResponse` / Mappings)
Ensure responses serialize the premium properties correctly back to the frontend:

```java
    // ... Existing RoomType fields ...

    private Boolean isPremium;
    private String premiumType; // Returns e.g. "FEMALE", "MALE", or null
```

---

## 4. Service Layer Updates (`com.nag.atithi.implementations.RoomTypeServiceImpl`)

Update the creation and modification operations to map the DTO properties to the Entity.

### A. Creation Map:
```java
RoomType roomType = RoomType.builder()
        // ... other mappings ...
        .isPremium(request.getIsPremium() != null ? request.getIsPremium() : false)
        .premiumType(Boolean.TRUE.equals(request.getIsPremium()) ? request.getPremiumType() : null)
        .build();
```

### B. Modification Map:
```java
if (request.getIsPremium() != null) {
    roomType.setIsPremium(request.getIsPremium());
}
if (Boolean.TRUE.equals(roomType.getIsPremium())) {
    roomType.setPremiumType(request.getPremiumType());
} else {
    roomType.setPremiumType(null); // Clean up if no longer premium
}
```

---

## 5. Test Suite Updates (`com.nag.atithi.tests`)

Update test resources to prevent failures and validate logic:

### A. Entity Unit Tests & Builder Mocking
Ensure mock fixtures (e.g. `RoomType.builder()`) initialize `isPremium` and `premiumType` appropriately. Update unit test cases asserting JSON serialization/deserialization to check for the custom enum mapping rules.

### B. Controller Integration Tests
Verify integration tests for `/api/v1/hotel/room-types` cover:
- Creating a room type with `isPremium = true` and `premiumType = FEMALE`.
- Updating a room type to toggle `isPremium = false` and asserting the `premium_type` column is nullified.
- Invalid requests (e.g., passing invalid premium option strings) trigger validation exceptions.
