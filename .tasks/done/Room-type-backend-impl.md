# Room Types Backend Implementation Guide

This guide details the backend implementation flow for the hotel room types REST CRUD endpoints required by the frontend client. The implementation matches the existing Spring Boot + JPA conventions, validation setups, response structures, and security patterns in the `atithi-server` project.

---

## 1. Database Migrations / Schema Changes

To allow customized room categories, pricing standards, occupancy rules, and amenities per hotel, apply the following DDL:

### A. Create `room_types` Table
Each hotel can customize its own room categories (e.g. Standard, Deluxe, Suite). Max adults, children, bedding, size, and pricing are stored on this table:
```sql
CREATE TABLE room_types (
    id BIGSERIAL PRIMARY KEY,
    hotel_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    base_price DECIMAL(12, 2) NOT NULL,
    max_adults INTEGER NOT NULL,
    max_children INTEGER NOT NULL DEFAULT 0,
    max_occupancy INTEGER NOT NULL,
    bed_type VARCHAR(100), -- Option A: stores bed type name string
    size VARCHAR(50),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_room_type_hotel 
        FOREIGN KEY (hotel_id) 
        REFERENCES hotels(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_room_type_name_per_hotel 
        UNIQUE (hotel_id, name)
);
```

### B. Create Join Table `room_type_amenities`
Create a many-to-many relationship join table to associate room types with their active amenities:
```sql
CREATE TABLE room_type_amenities (
    room_type_id BIGINT NOT NULL,
    amenity_id BIGINT NOT NULL,
    PRIMARY KEY (room_type_id, amenity_id),
    CONSTRAINT fk_room_type_amenity_room_type 
        FOREIGN KEY (room_type_id) 
        REFERENCES room_types(id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_room_type_amenity_amenity 
        FOREIGN KEY (amenity_id) 
        REFERENCES amenities(id) 
        ON DELETE CASCADE
);
```

---

## 2. Java Layer Implementations

Follow the Spring Boot project structure under package `com.nag.atithi`:

### A. Enumeration (`com.nag.atithi.enums.RoomTypeStatus`)
```java
package com.nag.atithi.enums;

public enum RoomTypeStatus {
    ACTIVE,
    INACTIVE
}
```

### B. Entity (`com.nag.atithi.models.RoomType`)
Create the model class:
```java
package com.nag.atithi.models;

import com.nag.atithi.enums.RoomTypeStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "room_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "max_adults", nullable = false)
    private Integer maxAdults;

    @Column(name = "max_children", nullable = false)
    private Integer maxChildren;

    @Column(name = "max_occupancy", nullable = false)
    private Integer maxOccupancy;

    @Column(name = "bed_type", length = 100)
    private String bedType;

    @Column(length = 50)
    private String size;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private RoomTypeStatus status = RoomTypeStatus.ACTIVE;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "room_type_amenities",
        joinColumns = @JoinColumn(name = "room_type_id"),
        inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    @Builder.Default
    private Set<Amenity> amenities = new HashSet<>();
}
```

---

## 3. Repository Layer (`com.nag.atithi.repositories.RoomTypeRepository`)

Define repository methods for fetching and querying room types:
```java
package com.nag.atithi.repositories;

import com.nag.atithi.models.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
    List<RoomType> findAllByHotelId(Long hotelId);
    Optional<RoomType> findByHotelIdAndId(Long hotelId, Long id);
    Optional<RoomType> findByHotelIdAndName(Long hotelId, String name);
}
```

---

## 4. Requests & DTOs

### A. Request Payload (`com.nag.atithi.requests.RoomTypeRequest`)
The parameters expected in both `POST` and `PUT` request bodies:
```java
package com.nag.atithi.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class RoomTypeRequest {
    
    @NotBlank(message = "Room type name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Base price is required")
    private BigDecimal basePrice;
    
    @NotNull(message = "Max adults is required")
    private Integer maxAdults;
    
    @NotNull(message = "Max children is required")
    private Integer maxChildren;
    
    @NotNull(message = "Max occupancy is required")
    private Integer maxOccupancy;
    
    private String bedType;
    
    private String size;
    
    private List<String> amenityIds;
    
    @NotBlank(message = "Status is required")
    private String status;
}
```

### B. Response DTO (`com.nag.atithi.responses.RoomTypeResponse`)
The formatted response DTO to send back to the client:
```java
package com.nag.atithi.responses;

import com.nag.atithi.models.Amenity;
import com.nag.atithi.models.RoomType;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public record RoomTypeResponse(
        Long id,
        String name,
        String description,
        BigDecimal basePrice,
        Integer maxAdults,
        Integer maxChildren,
        Integer maxOccupancy,
        String bedType,
        String size,
        String status,
        List<String> amenityIds
) {
    public static RoomTypeResponse fromEntity(RoomType entity) {
        List<String> amenityIds = entity.getAmenities().stream()
                .map(Amenity::getCode)
                .collect(Collectors.toList());

        return new RoomTypeResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getBasePrice(),
                entity.getMaxAdults(),
                entity.getMaxChildren(),
                entity.getMaxOccupancy(),
                entity.getBedType(),
                entity.getSize(),
                entity.getStatus().name(),
                amenityIds
        );
    }
}
```

---

## 5. Service Layer

### Service Interface (`com.nag.atithi.services.RoomTypeService`)
```java
package com.nag.atithi.services;

import com.nag.atithi.requests.RoomTypeRequest;
import com.nag.atithi.responses.RoomTypeResponse;

import java.util.List;

public interface RoomTypeService {
    List<RoomTypeResponse> getHotelRoomTypes(Long hotelId);
    RoomTypeResponse createRoomType(Long hotelId, RoomTypeRequest request);
    RoomTypeResponse updateRoomType(Long hotelId, Long id, RoomTypeRequest request);
    void deleteRoomType(Long hotelId, Long id);
}
```

### Implementation (`com.nag.atithi.implementations.RoomTypeServiceImpl`)
This service implementation provides transaction management and JPA record mappings:
```java
package com.nag.atithi.implementations;

import com.nag.atithi.enums.RoomTypeStatus;
import com.nag.atithi.models.Amenity;
import com.nag.atithi.models.Hotel;
import com.nag.atithi.models.RoomType;
import com.nag.atithi.repositories.AmenityRepository;
import com.nag.atithi.repositories.HotelRepository;
import com.nag.atithi.repositories.RoomTypeRepository;
import com.nag.atithi.requests.RoomTypeRequest;
import com.nag.atithi.responses.RoomTypeResponse;
import com.nag.atithi.services.RoomTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomTypeServiceImpl implements RoomTypeService {

    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final AmenityRepository amenityRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RoomTypeResponse> getHotelRoomTypes(Long hotelId) {
        return roomTypeRepository.findAllByHotelId(hotelId).stream()
                .map(RoomTypeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RoomTypeResponse createRoomType(Long hotelId, RoomTypeRequest request) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Hotel not found with id: " + hotelId));

        RoomType roomType = RoomType.builder()
                .hotel(hotel)
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .basePrice(request.getBasePrice())
                .maxAdults(request.getMaxAdults())
                .maxChildren(request.getMaxChildren())
                .maxOccupancy(request.getMaxOccupancy())
                .bedType(request.getBedType() != null ? request.getBedType().trim() : null)
                .size(request.getSize() != null ? request.getSize().trim() : null)
                .status(RoomTypeStatus.valueOf(request.getStatus()))
                .build();

        if (request.getAmenityIds() != null && !request.getAmenityIds().isEmpty()) {
            List<Amenity> amenities = amenityRepository.findAllByCodeIn(request.getAmenityIds());
            roomType.setAmenities(new HashSet<>(amenities));
        }

        RoomType saved = roomTypeRepository.save(roomType);
        return RoomTypeResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public RoomTypeResponse updateRoomType(Long hotelId, Long id, RoomTypeRequest request) {
        RoomType roomType = roomTypeRepository.findByHotelIdAndId(hotelId, id)
                .orElseThrow(() -> new RuntimeException("Room type not found with id: " + id));

        roomType.setName(request.getName().trim());
        roomType.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        roomType.setBasePrice(request.getBasePrice());
        roomType.setMaxAdults(request.getMaxAdults());
        roomType.setMaxChildren(request.getMaxChildren());
        roomType.setMaxOccupancy(request.getMaxOccupancy());
        roomType.setBedType(request.getBedType() != null ? request.getBedType().trim() : null);
        roomType.setSize(request.getSize() != null ? request.getSize().trim() : null);
        roomType.setStatus(RoomTypeStatus.valueOf(request.getStatus()));

        if (request.getAmenityIds() != null) {
            List<Amenity> amenities = amenityRepository.findAllByCodeIn(request.getAmenityIds());
            roomType.getAmenities().clear();
            roomType.getAmenities().addAll(amenities);
        } else {
            roomType.getAmenities().clear();
        }

        RoomType saved = roomTypeRepository.save(roomType);
        return RoomTypeResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void deleteRoomType(Long hotelId, Long id) {
        RoomType roomType = roomTypeRepository.findByHotelIdAndId(hotelId, id)
                .orElseThrow(() -> new RuntimeException("Room type not found with id: " + id));
        roomTypeRepository.delete(roomType);
    }
}
```

---

## 6. Controller Layer (`com.nag.atithi.controllers.RoomTypesController`)

Implement API endpoints mapping to `@RequestMapping("/api/v1/hotel/room-types")`:
```java
package com.nag.atithi.controllers;

import com.nag.atithi.models.UserPrincipal;
import com.nag.atithi.requests.RoomTypeRequest;
import com.nag.atithi.responses.ApiResponse;
import com.nag.atithi.responses.RoomTypeResponse;
import com.nag.atithi.services.RoomTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hotel/room-types")
@RequiredArgsConstructor
public class RoomTypesController {

    private final RoomTypeService roomTypeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomTypeResponse>>> getRoomTypes(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        if (userPrincipal == null || userPrincipal.getHotelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        List<RoomTypeResponse> roomTypes = roomTypeService.getHotelRoomTypes(userPrincipal.getHotelId());
        return ResponseEntity.ok(ApiResponse.success("Room types fetched successfully", roomTypes));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoomTypeResponse>> createRoomType(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody RoomTypeRequest request
    ) {
        if (userPrincipal == null || userPrincipal.getHotelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        RoomTypeResponse response = roomTypeService.createRoomType(userPrincipal.getHotelId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Room type created successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomTypeResponse>> updateRoomType(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody RoomTypeRequest request
    ) {
        if (userPrincipal == null || userPrincipal.getHotelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        RoomTypeResponse response = roomTypeService.updateRoomType(userPrincipal.getHotelId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Room type updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoomType(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        if (userPrincipal == null || userPrincipal.getHotelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        roomTypeService.deleteRoomType(userPrincipal.getHotelId(), id);
        return ResponseEntity.ok(ApiResponse.success("Room type deleted successfully", null));
    }
}
```
