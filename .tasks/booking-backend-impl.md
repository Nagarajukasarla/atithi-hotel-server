# Backend Booking Implementation Flow: Spring Boot Specification

This document details the Java Spring Boot backend implementation flow for walk-in room bookings. It covers the database schema (SQL), JPA Entity models, repositories, request payloads, business service interfaces/implementations with transactional boundaries, and API controllers.

---

## 1. Database Schema Design (Liquibase / SQL)

The tables `bookings` and `booking_guests` store walk-in bookings and their corresponding guests.

```sql
-- 1. Booking Entity Parent Table
CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hotel_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    check_in TIMESTAMP NOT NULL,
    check_out TIMESTAMP NOT NULL,
    is_hourly BOOLEAN NOT NULL DEFAULT FALSE,
    total_price DOUBLE PRECISION NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (hotel_id) REFERENCES hotels(id) ON DELETE CASCADE,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE RESTRICT
);

-- 2. Booking Guests Details Child Table
CREATE TABLE booking_guests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    aadhaar_number VARCHAR(12) NOT NULL,
    mobile_number VARCHAR(15) NOT NULL,
    age INT,
    address TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
);
```

---

## 2. JPA Models & Enums

### A. BookingStatus Enum
```java
package com.nag.atithi.enums;

public enum BookingStatus {
    CONFIRMED,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELLED
}
```

### B. Booking Entity
```java
package com.nag.atithi.models;

import com.nag.atithi.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "check_in", nullable = false)
    private LocalDateTime checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDateTime checkOut;

    @Column(name = "is_hourly", nullable = false)
    private Boolean isHourly;

    @Column(name = "total_price", nullable = false)
    private Double totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private BookingStatus status = BookingStatus.CONFIRMED;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BookingGuest> guests = new ArrayList<>();
}
```

### C. BookingGuest Entity
```java
package com.nag.atithi.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "booking_guests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingGuest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "aadhaar_number", nullable = false, length = 12)
    private String aadhaarNumber;

    @Column(name = "mobile_number", nullable = false, length = 15)
    private String mobileNumber;

    private Integer age;

    @Column(columnDefinition = "TEXT")
    private String address;
}
```

---

## 3. Repository Layer

Uses Spring Data JPA to manage database access objects.

```java
// 1. Booking Repository
package com.nag.atithi.repositories;

import com.nag.atithi.models.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findAllByHotelId(Long hotelId);
}

// 2. BookingGuest Repository
package com.nag.atithi.repositories;

import com.nag.atithi.models.BookingGuest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingGuestRepository extends JpaRepository<BookingGuest, Long> {
}
```

---

## 4. Request Payloads (DTOs)

Request payloads match the frontend `booking-page` submission parameters.

```java
// 1. GuestRequest.java
package com.nag.atithi.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class GuestRequest {
    @NotBlank(message = "Guest name is required")
    private String name;

    @NotBlank(message = "Aadhaar number is required")
    @Pattern(regexp = "^\\d{12}$", message = "Aadhaar number must be exactly 12 digits")
    private String aadhaarNumber;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^\\d{10,15}$", message = "Mobile number must be between 10 and 15 digits")
    private String mobileNumber;

    private Integer age;
    private String address;
}

// 2. CreateBookingRequest.java
package com.nag.atithi.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateBookingRequest {
    @NotNull(message = "Room ID is required")
    private Long roomId;

    @NotNull(message = "Check-in time is required")
    private LocalDateTime checkIn;

    @NotNull(message = "Check-out time is required")
    private LocalDateTime checkOut;

    @NotNull(message = "isHourly flag is required")
    private Boolean isHourly;

    @NotEmpty(message = "At least one guest detail is required")
    @Valid
    private List<GuestRequest> guests;
}
```

---

## 5. Service Layer (Business Logic)

Coordinates booking entity storage, bulk guests allocation, room availability verification, and room status updates within a **single transaction block** (`@Transactional`).

```java
// 1. BookingService.java
package com.nag.atithi.services;

import com.nag.atithi.models.Booking;
import com.nag.atithi.requests.CreateBookingRequest;

public interface BookingService {
    Booking createBooking(Long hotelId, CreateBookingRequest request);
}

// 2. BookingServiceImpl.java
package com.nag.atithi.implementations;

import com.nag.atithi.enums.BookingStatus;
import com.nag.atithi.enums.RoomStatus;
import com.nag.atithi.models.*;
import com.nag.atithi.repositories.*;
import com.nag.atithi.requests.CreateBookingRequest;
import com.nag.atithi.services.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;

    @Override
    @Transactional
    public Booking createBooking(Long hotelId, CreateBookingRequest request) {
        // 1. Fetch Hotel
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Hotel not found with id: " + hotelId));

        // 2. Fetch Room (using findByIdAndHotelId to verify ownership)
        Room room = roomRepository.findByIdAndHotelId(request.getRoomId(), hotelId)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + request.getRoomId()));

        // 3. Business Rule Validation: Room status must be VACANT
        if (room.getRoomStatus() != RoomStatus.VACANT) {
            throw new IllegalStateException("Room #" + room.getRoomNumber() + " is currently " + room.getRoomStatus() + " (not Vacant).");
        }

        // 4. Calculate total booking price based on RoomType basePrice configuration
        Double totalPrice = calculateTotalPrice(room, request);

        // 5. Build Booking Entity
        Booking booking = Booking.builder()
                .hotel(hotel)
                .room(room)
                .checkIn(request.getCheckIn())
                .checkOut(request.getCheckOut())
                .isHourly(request.getIsHourly())
                .totalPrice(totalPrice)
                .status(BookingStatus.CONFIRMED)
                .build();

        // 6. Map Guest Requests to Entities
        List<BookingGuest> guests = request.getGuests().stream()
                .map(gRequest -> BookingGuest.builder()
                        .booking(booking)
                        .name(gRequest.getName().trim())
                        .aadhaarNumber(gRequest.getAadhaarNumber().trim())
                        .mobileNumber(gRequest.getMobileNumber().trim())
                        .age(gRequest.getAge())
                        .address(gRequest.getAddress() != null ? gRequest.getAddress().trim() : null)
                        .build())
                .collect(Collectors.toList());

        booking.setGuests(guests);

        // 7. Update Room Status from VACANT to OCCUPIED
        room.setRoomStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room); // Saves room status change

        // 8. Save Booking (CascadeType.ALL will automatically save the mapped guests list)
        return bookingRepository.save(booking);
    }

    private Double calculateTotalPrice(Room room, CreateBookingRequest request) {
        Duration duration = Duration.between(request.getCheckIn(), request.getCheckOut());
        long diffSeconds = duration.getSeconds();
        if (diffSeconds <= 0) {
            throw new IllegalArgumentException("Check-out time must be after check-in time.");
        }

        double basePrice = room.getRoomType().getBasePrice().doubleValue();

        if (request.getIsHourly()) {
            double hours = Math.ceil(diffSeconds / 3600.0);
            double hourlyRate = Math.round(basePrice / 10.0);
            return hours * hourlyRate;
        } else {
            double days = Math.ceil(diffSeconds / 86400.0);
            return days * basePrice;
        }
    }
}
```

---

## 6. Controller Layer

API router receiving requests, performing validation, and executing services.

```java
package com.nag.atithi.controllers;

import com.nag.atithi.models.Booking;
import com.nag.atithi.models.UserPrincipal;
import com.nag.atithi.requests.CreateBookingRequest;
import com.nag.atithi.responses.ApiResponse;
import com.nag.atithi.services.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/hotel/bookings")
@RequiredArgsConstructor
public class HotelBookingsController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<Booking>> createBooking(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateBookingRequest request
    ) {
        if (userPrincipal == null || userPrincipal.getHotelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        Booking booking = bookingService.createBooking(userPrincipal.getHotelId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created successfully", booking));
    }
}
```
