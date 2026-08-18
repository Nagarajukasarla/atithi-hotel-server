# Backend Implementation Plan - Check Availability Service

This document defines the backend implementation plan for the room availability check functionality, resolving queries based on active room types, vacant status, and requested occupancy capacity.

---

## 1. Request DTO (`CheckAvailabilityRequest.java`)

We will create a request DTO to bind and validate JSON request parameters sent from the frontend `bookingService.checkAvailability` method.

* **Target File**: `com/nag/atithi/requests/CheckAvailabilityRequest.java` [NEW]
* **Implementation Details**:
```java
package com.nag.atithi.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckAvailabilityRequest {
    @NotBlank(message = "Room type is required")
    private String roomType;

    @NotNull(message = "Capacity/occupancy is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer maxOccupancy;

    @NotBlank(message = "Status filter is required")
    private String status;
}
```

---

## 2. Repository Layer (`RoomRepository.java`)

We will add a custom JPQL query to fetch rooms that are associated with the selected hotel, match the requested category name, have a capacity equal to or greater than the query size, and have a specific operational status (e.g. `VACANT`).

* **Target File**: `com/nag/atithi/repositories/RoomRepository.java` [MODIFY]
* **Implementation Details**:
```java
package com.nag.atithi.repositories;

import com.nag.atithi.enums.RoomStatus;
import com.nag.atithi.models.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findAllByHotelIdOrderByRoomNumberAsc(Long hotelId);
    boolean existsByHotelIdAndRoomNumber(Long hotelId, String roomNumber);
    Optional<Room> findByIdAndHotelId(Long id, Long hotelId);

    @Query("SELECT r FROM Room r JOIN FETCH r.roomType rt WHERE r.hotel.id = :hotelId " +
           "AND rt.name = :roomTypeName AND r.capacity >= :capacity AND r.roomStatus = :roomStatus")
    List<Room> findAvailableRooms(
        @Param("hotelId") Long hotelId,
        @Param("roomTypeName") String roomTypeName,
        @Param("capacity") Integer capacity,
        @Param("roomStatus") RoomStatus roomStatus
    );
}
```

---

## 3. Service Layer (`RoomService.java` & `RoomServiceImpl.java`)

We will add the checking method signature to `RoomService` and implement it in `RoomServiceImpl`, converting the string status into the JVM `RoomStatus` enum.

### `RoomService.java` [MODIFY]
* Add the following signature:
```java
List<Room> checkAvailability(Long hotelId, CheckAvailabilityRequest request);
```

### `RoomServiceImpl.java` [MODIFY]
* Implement the service method using the repository custom query:
```java
    @Override
    @Transactional(readOnly = true)
    public List<Room> checkAvailability(Long hotelId, CheckAvailabilityRequest request) {
        RoomStatus status = RoomStatus.fromValue(request.getStatus().trim());
        return roomRepository.findAvailableRooms(
            hotelId,
            request.getRoomType().trim(),
            request.getMaxOccupancy(),
            status
        );
    }
```

---

## 4. Controller Layer (`HotelRoomsController.java`)

We will expose the `POST` endpoint `/api/v1/hotel/rooms/available` returning the list of matching available rooms formatted as `RoomResponse` objects.

* **Target File**: `com/nag/atithi/controllers/HotelRoomsController.java` [MODIFY]
* **Implementation Details**:
```java
    @PostMapping("/available")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> checkAvailability(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CheckAvailabilityRequest request
    ) {
        if (userPrincipal == null || userPrincipal.getHotelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        List<Room> rooms = roomService.checkAvailability(userPrincipal.getHotelId(), request);
        List<RoomResponse> responses = rooms.stream()
                .map(RoomResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Available rooms checked successfully", responses));
    }
```

---

## 5. Verification Plan

### Automated Tests
* Validate compile correctness of all Java modules:
  ```bash
  mvn clean compile
  ```
* Write integration tests under `src/test/java` to verify queries return only vacant rooms matching specified roomType and capacity filters.
