# Rooms Backend Implementation Guide

This guide details the backend implementation flow for the hotel rooms endpoints required by the frontend client. The implementation matches the existing Spring Boot + JPA conventions, validation setups, response structures, and security patterns in the `atithi-server` project.

---

## 1. Database Migrations / Schema Changes

Apply the following DDL migration to create the `rooms` table. This stores individual rooms with their numbers, floors, capacity, and references both the `hotels` and `room_types` tables.

```sql
CREATE TABLE rooms (
    id BIGSERIAL PRIMARY KEY,
    hotel_id BIGINT NOT NULL,
    room_number VARCHAR(50) NOT NULL,
    floor VARCHAR(50) NOT NULL,
    room_type_id BIGINT NOT NULL,
    capacity INTEGER NOT NULL,
    room_status VARCHAR(30) NOT NULL DEFAULT 'VACANT',
    room_state VARCHAR(30) NOT NULL DEFAULT 'CLEAN',
    last_cleaned TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_room_hotel 
        FOREIGN KEY (hotel_id) 
        REFERENCES hotels(id)
        ON DELETE CASCADE,
        
    CONSTRAINT fk_room_room_type 
        FOREIGN KEY (room_type_id) 
        REFERENCES room_types(id)
        ON DELETE RESTRICT,
        
    CONSTRAINT uq_room_number_per_hotel 
        UNIQUE (hotel_id, room_number)
);
```

---

## 2. Java Layer Implementations

Follow the Spring Boot project structure under package `com.nag.atithi`:

### A. Room Status Enum (`com.nag.atithi.enums.RoomStatus`)
To bridge the database representation (uppercase, e.g., `VACANT`) and the frontend expectations (spaced PascalCase, e.g., `"Vacant"`, `"Stay Over"`), we use Jackson annotations `@JsonValue` and `@JsonCreator`:

```java
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
```

### B. Room State Enum (`com.nag.atithi.enums.RoomState`)
Likewise, map room conditions cleanly:

```java
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
```

### C. Entity (`com.nag.atithi.models.Room`)
Create the JPA model class inheriting from `BaseEntity`:

```java
package com.nag.atithi.models;

import com.nag.atithi.enums.RoomState;
import com.nag.atithi.enums.RoomStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(name = "room_number", nullable = false, length = 50)
    private String roomNumber;

    @Column(nullable = false, length = 50)
    private String floor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @Column(nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_status", nullable = false, length = 30)
    @Builder.Default
    private RoomStatus roomStatus = RoomStatus.VACANT;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_state", nullable = false, length = 30)
    @Builder.Default
    private RoomState roomState = RoomState.CLEAN;

    @Column(name = "last_cleaned")
    private LocalDateTime lastCleaned;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
```

---

## 3. Repository Layer (`com.nag.atithi.repositories.RoomRepository`)

Define JPA queries to fetch rooms secure to a specific hotel, find by ID + Hotel, and evaluate uniqueness constraints:

```java
package com.nag.atithi.repositories;

import com.nag.atithi.models.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findAllByHotelIdOrderByIdDesc(Long hotelId);
    Optional<Room> findByIdAndHotelId(Long id, Long hotelId);
    boolean existsByHotelIdAndRoomNumber(Long hotelId, String roomNumber);
}
```

---

## 4. Requests & Responses (DTOs)

### A. Create Request (`com.nag.atithi.requests.CreateRoomRequest`)
Used for `POST /api/v1/hotel/rooms`:

```java
package com.nag.atithi.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRoomRequest {

    @NotBlank(message = "Room number is required")
    private String number;

    @NotBlank(message = "Floor position is required")
    private String floor;

    @NotBlank(message = "Room category/type is required")
    private String roomType;

    @NotNull(message = "Maximum capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1 guest")
    private Integer capacity;

    private String notes;
}
```

### B. Update Request (`com.nag.atithi.requests.UpdateRoomRequest`)
Used for `PUT /api/v1/hotel/rooms/{id}`:

```java
package com.nag.atithi.requests;

import com.nag.atithi.enums.RoomState;
import com.nag.atithi.enums.RoomStatus;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRoomRequest {

    private String number;

    private String floor;

    private String roomType;

    @Min(value = 1, message = "Capacity must be at least 1 guest")
    private Integer capacity;

    private RoomStatus roomStatus;

    private RoomState roomState;

    private String notes;
}
```

### C. Response Wrapper (`com.nag.atithi.responses.RoomResponse`)
Flattens references and ensures serialization matches frontend field names (`number`, `roomType` as string, and string representations of enums/dates):

```java
package com.nag.atithi.responses;

import com.nag.atithi.models.Room;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@Builder
public class RoomResponse {
    private String id;
    private String number;
    private String floor;
    private String roomType;
    private Integer capacity;
    private String roomStatus;
    private String roomState;
    private String lastCleaned;
    private String notes;

    public static RoomResponse fromEntity(Room room) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        
        return RoomResponse.builder()
                .id(String.valueOf(room.getId()))
                .number(room.getRoomNumber())
                .floor(room.getFloor())
                .roomType(room.getRoomType() != null ? room.getRoomType().getName() : null)
                .capacity(room.getCapacity())
                .roomStatus(room.getRoomStatus() != null ? room.getRoomStatus().getValue() : null)
                .roomState(room.getRoomState() != null ? room.getRoomState().getValue() : null)
                .lastCleaned(room.getLastCleaned() != null ? room.getLastCleaned().format(formatter) : null)
                .notes(room.getNotes())
                .build();
    }
}
```

---

## 5. Service Layer

### Service Interface (`com.nag.atithi.services.RoomService`)

```java
package com.nag.atithi.services;

import com.nag.atithi.models.Room;
import com.nag.atithi.requests.CreateRoomRequest;
import com.nag.atithi.requests.UpdateRoomRequest;

import java.util.List;

public interface RoomService {
    List<Room> getHotelRooms(Long hotelId);
    Room createRoom(Long hotelId, CreateRoomRequest request);
    Room updateRoom(Long hotelId, Long roomId, UpdateRoomRequest request);
    void deleteRoom(Long hotelId, Long roomId);
}
```

### Implementation (`com.nag.atithi.implementations.RoomServiceImpl`)

```java
package com.nag.atithi.implementations;

import com.nag.atithi.enums.RoomState;
import com.nag.atithi.enums.RoomStatus;
import com.nag.atithi.models.Hotel;
import com.nag.atithi.models.Room;
import com.nag.atithi.models.RoomType;
import com.nag.atithi.repositories.HotelRepository;
import com.nag.atithi.repositories.RoomRepository;
import com.nag.atithi.repositories.RoomTypeRepository;
import com.nag.atithi.requests.CreateRoomRequest;
import com.nag.atithi.requests.UpdateRoomRequest;
import com.nag.atithi.services.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Room> getHotelRooms(Long hotelId) {
        return roomRepository.findAllByHotelIdOrderByIdDesc(hotelId);
    }

    @Override
    @Transactional
    public Room createRoom(Long hotelId, CreateRoomRequest request) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Hotel not found with id: " + hotelId));

        String roomNumber = request.getNumber().trim();
        // Validation: Unique room number per hotel
        if (roomRepository.existsByHotelIdAndRoomNumber(hotelId, roomNumber)) {
            throw new IllegalArgumentException("Room number '" + roomNumber + "' already exists in this hotel.");
        }

        // Fetch RoomType config by name
        RoomType roomType = roomTypeRepository.findByHotelIdAndName(hotelId, request.getRoomType().trim())
                .orElseThrow(() -> new RuntimeException("Room category '" + request.getRoomType() + "' not found."));

        Room room = Room.builder()
                .hotel(hotel)
                .roomNumber(roomNumber)
                .floor(request.getFloor().trim())
                .roomType(roomType)
                .capacity(request.getCapacity())
                .roomStatus(RoomStatus.VACANT)
                .roomState(RoomState.CLEAN)
                .lastCleaned(LocalDateTime.now())
                .notes(request.getNotes() != null ? request.getNotes().trim() : null)
                .build();

        return roomRepository.save(room);
    }

    @Override
    @Transactional
    public Room updateRoom(Long hotelId, Long roomId, UpdateRoomRequest request) {
        Room room = roomRepository.findByIdAndHotelId(roomId, hotelId)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + roomId));

        // Validation: room number uniqueness check upon change
        if (request.getNumber() != null && !request.getNumber().trim().isEmpty()) {
            String newNumber = request.getNumber().trim();
            if (!newNumber.equalsIgnoreCase(room.getRoomNumber()) &&
                    roomRepository.existsByHotelIdAndRoomNumber(hotelId, newNumber)) {
                throw new IllegalArgumentException("Room number '" + newNumber + "' already exists in this hotel.");
            }
            room.setRoomNumber(newNumber);
        }

        if (request.getFloor() != null) {
            room.setFloor(request.getFloor().trim());
        }

        if (request.getRoomType() != null && !request.getRoomType().trim().isEmpty()) {
            RoomType roomType = roomTypeRepository.findByHotelIdAndName(hotelId, request.getRoomType().trim())
                    .orElseThrow(() -> new RuntimeException("Room category '" + request.getRoomType() + "' not found."));
            room.setRoomType(roomType);
        }

        if (request.getCapacity() != null) {
            room.setCapacity(request.getCapacity());
        }

        if (request.getRoomStatus() != null) {
            room.setRoomStatus(request.getRoomStatus());
        }

        if (request.getRoomState() != null) {
            room.setRoomState(request.getRoomState());
            // Business rule: automatically update timestamp when marked clean
            if (request.getRoomState() == RoomState.CLEAN) {
                room.setLastCleaned(LocalDateTime.now());
            }
        }

        if (request.getNotes() != null) {
            room.setNotes(request.getNotes().trim());
        }

        return roomRepository.save(room);
    }

    @Override
    @Transactional
    public void deleteRoom(Long hotelId, Long roomId) {
        Room room = roomRepository.findByIdAndHotelId(roomId, hotelId)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + roomId));
        roomRepository.delete(room);
    }
}
```

---

## 6. Controller Layer (`com.nag.atithi.controllers.HotelRoomsController`)

Maps REST endpoints with security context injections to extract the authenticated user's hotel ID securely:

```java
package com.nag.atithi.controllers;

import com.nag.atithi.models.Room;
import com.nag.atithi.models.UserPrincipal;
import com.nag.atithi.requests.CreateRoomRequest;
import com.nag.atithi.requests.UpdateRoomRequest;
import com.nag.atithi.responses.ApiResponse;
import com.nag.atithi.responses.RoomResponse;
import com.nag.atithi.services.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/hotel/rooms")
@RequiredArgsConstructor
public class HotelRoomsController {

    private final RoomService roomService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getRooms(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        if (userPrincipal == null || userPrincipal.getHotelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        List<Room> rooms = roomService.getHotelRooms(userPrincipal.getHotelId());
        List<RoomResponse> responses = rooms.stream()
                .map(RoomResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Hotel rooms fetched successfully", responses));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateRoomRequest request
    ) {
        if (userPrincipal == null || userPrincipal.getHotelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        Room room = roomService.createRoom(userPrincipal.getHotelId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Room created successfully", RoomResponse.fromEntity(room)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoomRequest request
    ) {
        if (userPrincipal == null || userPrincipal.getHotelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        Room room = roomService.updateRoom(userPrincipal.getHotelId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Room updated successfully", RoomResponse.fromEntity(room)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        if (userPrincipal == null || userPrincipal.getHotelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        roomService.deleteRoom(userPrincipal.getHotelId(), id);
        return ResponseEntity.ok(ApiResponse.success("Room deleted successfully", null));
    }
}
```
