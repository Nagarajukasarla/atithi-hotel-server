# Amenities Backend Implementation Guide

This guide details the backend implementation flow for the hotel amenities endpoints required by the frontend settings client. The implementation matches the existing Spring Boot + JPA conventions, validation setups, response structures, and security patterns in the `atithi-server` project.

---

## 1. Database Migrations / Schema Changes

To allow programmatic identification of amenities (mapping the frontend string IDs like `"wifi"` or `"ac"` to database rows) and link them to hotels, apply these schema modifications:

### A. Add Code Column to `amenities` Table
Add a unique code column to the `amenities` table to serve as a machine-readable identifier:
```sql
ALTER TABLE amenities ADD COLUMN code VARCHAR(50) NOT NULL UNIQUE;
```

### B. Create Join Table `hotel_amenities`
Create a many-to-many relationship join table to associate hotels with their active amenities:
```sql
CREATE TABLE hotel_amenities (
    hotel_id BIGINT NOT NULL,
    amenity_id BIGINT NOT NULL,
    PRIMARY KEY (hotel_id, amenity_id),
    CONSTRAINT fk_hotel_amenity_hotel 
        FOREIGN KEY (hotel_id) 
        REFERENCES hotels(id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_hotel_amenity_amenity 
        FOREIGN KEY (amenity_id) 
        REFERENCES amenities(id) 
        ON DELETE CASCADE
);
```

---

## 2. Java Layer Implementations

Follow the Spring Boot project structure under package `com.nag.atithi`:

### A. Enumeration (`com.nag.atithi.enums.AmenityStatus`)
If not already present, define an enum for the status:
```java
package com.nag.atithi.enums;

public enum AmenityStatus {
    ACTIVE,
    INACTIVE
}
```

### B. Entity (`com.nag.atithi.models.Amenity`)
Create the JPA model inheriting from `BaseEntity`:
```java
package com.nag.atithi.models;

import com.nag.atithi.enums.AmenityStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "amenities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Amenity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String icon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private AmenityStatus status = AmenityStatus.ACTIVE;
}
```

### C. Entity Modification (`com.nag.atithi.models.Hotel`)
Add the many-to-many relationship mapping inside `Hotel.java`:
```java
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "hotel_amenities",
        joinColumns = @JoinColumn(name = "hotel_id"),
        inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    @Builder.Default
    private java.util.Set<Amenity> amenities = new java.util.HashSet<>();
```

---

## 3. Repository Layer (`com.nag.atithi.repositories.AmenityRepository`)

Create the data access interface:
```java
package com.nag.atithi.repositories;

import com.nag.atithi.models.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Long> {
    Optional<Amenity> findByCode(String code);
    List<Amenity> findAllByCodeIn(Collection<String> codes);
}
```

---

## 4. Requests & DTOs

### DTO Payload (`com.nag.atithi.requests.UpdateAmenitiesRequest`)
Define the payload expected in the `POST` request body:
```java
package com.nag.atithi.requests;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateAmenitiesRequest {
    
    @NotNull(message = "Amenities list cannot be null")
    private List<String> amenities;
}
```

---

## 5. Service Layer

### Service Interface (`com.nag.atithi.services.AmenityService`)
```java
package com.nag.atithi.services;

import java.util.List;

public interface AmenityService {
    List<String> getHotelAmenities(Long hotelId);
    void updateHotelAmenities(Long hotelId, List<String> amenityCodes);
}
```

### Implementation (`com.nag.atithi.implementations.AmenityServiceImpl`)
Implement service operations with transaction control and repository mapping:
```java
package com.nag.atithi.implementations;

import com.nag.atithi.models.Amenity;
import com.nag.atithi.models.Hotel;
import com.nag.atithi.repositories.AmenityRepository;
import com.nag.atithi.repositories.HotelRepository;
import com.nag.atithi.services.AmenityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AmenityServiceImpl implements AmenityService {

    private final HotelRepository hotelRepository;
    private final AmenityRepository amenityRepository;

    @Override
    @Transactional(readOnly = true)
    public List<String> getHotelAmenities(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Hotel not found with id: " + hotelId));
        
        return hotel.getAmenities().stream()
                .map(Amenity::getCode)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateHotelAmenities(Long hotelId, List<String> amenityCodes) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Hotel not found with id: " + hotelId));

        List<Amenity> matchingAmenities = amenityRepository.findAllByCodeIn(amenityCodes);
        
        // Clear previous associations and apply the new set
        hotel.getAmenities().clear();
        hotel.getAmenities().addAll(new HashSet<>(matchingAmenities));

        hotelRepository.save(hotel);
    }
}
```

---

## 6. Controller Layer (`com.nag.atithi.controllers.HotelAmenitiesController`)

Implement API endpoints mapping to `@RequestMapping("/api/v1/hotel/amenities")`:
```java
package com.nag.atithi.controllers;

import com.nag.atithi.models.UserPrincipal;
import com.nag.atithi.requests.UpdateAmenitiesRequest;
import com.nag.atithi.responses.ApiResponse;
import com.nag.atithi.services.AmenityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hotel/amenities")
@RequiredArgsConstructor
public class HotelAmenitiesController {

    private final AmenityService amenityService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<String>>> getAmenities(
            // Spring Security automatically injects the authenticated UserPrincipal from the SecurityContext
            // (populated by the JWT Authentication Filter). The frontend does NOT need to pass this.
            // This is used to securely identify the current user's hotel ID without path parameter tampering.
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        // Defensive check for safety against mock tests or context misconfigurations.
        // Spring Security JWT filters already handle blocking unauthenticated HTTP requests.
        if (userPrincipal == null || userPrincipal.getHotelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        List<String> selectedCodes = amenityService.getHotelAmenities(userPrincipal.getHotelId());
        return ResponseEntity.ok(ApiResponse.success("Hotel amenities fetched successfully", selectedCodes));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> saveAmenities(
            // Spring Security automatically injects the authenticated UserPrincipal.
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            // The JSON body containing the list of selected amenities sent from the frontend.
            @Valid @RequestBody UpdateAmenitiesRequest request
    ) {
        if (userPrincipal == null || userPrincipal.getHotelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        amenityService.updateHotelAmenities(userPrincipal.getHotelId(), request.getAmenities());
        return ResponseEntity.ok(ApiResponse.success("Hotel amenities saved successfully", null));
    }
}
```
```
