# Bed Types Backend Implementation Guide

This guide details the backend implementation flow for the hotel bed types endpoints required by the frontend client. The implementation matches the existing Spring Boot + JPA conventions, validation setups, response structures, and security patterns in the `atithi-server` project.

---

## 1. Database Migrations / Schema Changes

To allow customized bedding types per hotel, apply the following DDL:

### A. Create `bed_types` Table
Each hotel can customize its own bed configurations. Max adults, children, and overall capacity are stored on this table:
```sql
CREATE TABLE bed_types (
    id BIGSERIAL PRIMARY KEY,
    hotel_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    capacity INTEGER NOT NULL,
    max_adults INTEGER NOT NULL,
    max_children INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bed_type_hotel 
        FOREIGN KEY (hotel_id) 
        REFERENCES hotels(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_bed_type_name_per_hotel 
        UNIQUE (hotel_id, name)
);
```

### B. Room Types Bedding Reference (Options)
The room types table currently has a `bed_type VARCHAR(50)` column. Depending on relational design preferences:

- **Option A (Simple - Recommended):** Keep `bed_type VARCHAR(50)` on the `room_types` table. The frontend select drop-down will save the *name* of the bed type (e.g. `"Double Bed"`, `"Maharani"`) as a string. This requires zero schema changes to the existing `room_types` table.
- **Option B (Foreign Key):** Drop the `bed_type` column and add `bed_type_id BIGINT REFERENCES bed_types(id)` to the `room_types` table.

---

## 2. Java Layer Implementations

Follow the Spring Boot project structure under package `com.nag.atithi`:

### A. Enumeration (`com.nag.atithi.enums.BedStatus`)
```java
package com.nag.atithi.enums;

public enum BedStatus {
    ACTIVE,
    INACTIVE
}
```

### B. Entity (`com.nag.atithi.models.BedType`)
Create the model class:
```java
package com.nag.atithi.models;

import com.nag.atithi.enums.BedStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bed_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BedType extends BaseEntity {

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

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "max_adults", nullable = false)
    private Integer maxAdults;

    @Column(name = "max_children", nullable = false)
    private Integer maxChildren;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private BedStatus status = BedStatus.ACTIVE;
}
```

---

## 3. Repository Layer (`com.nag.atithi.repositories.BedTypeRepository`)

Define repository methods for fetching and querying bed types:
```java
package com.nag.atithi.repositories;

import com.nag.atithi.models.BedType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BedTypeRepository extends JpaRepository<BedType, Long> {
    List<BedType> findAllByHotelId(Long hotelId);
    Optional<BedType> findByHotelIdAndName(Long hotelId, String name);
}
```

---

## 4. Requests & DTOs

### A. Request DTO (`com.nag.atithi.requests.BedTypeRequest`)
The individual bed type details passed in the list:
```java
package com.nag.atithi.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BedTypeRequest {
    
    private String id; // Receives e.g., "1" or client-side temp UUID "bed-171827"
    
    @NotBlank(message = "Bed type name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Max adults is required")
    private Integer maxAdults;
    
    @NotNull(message = "Max children is required")
    private Integer maxChildren;
    
    @NotNull(message = "Capacity is required")
    private Integer capacity;
}
```

### B. List Wrapper DTO (`com.nag.atithi.requests.UpdateBedsRequest`)
```java
package com.nag.atithi.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateBedsRequest {
    
    @NotNull(message = "Beds list cannot be null")
    @Valid
    private List<BedTypeRequest> beds;
}
```

---

## 5. Service Layer

### Service Interface (`com.nag.atithi.services.BedTypeService`)
```java
package com.nag.atithi.services;

import com.nag.atithi.models.BedType;
import com.nag.atithi.requests.BedTypeRequest;

import java.util.List;

public interface BedTypeService {
    List<BedType> getHotelBeds(Long hotelId);
    void updateHotelBeds(Long hotelId, List<BedTypeRequest> bedsList);
}
```

### Implementation (`com.nag.atithi.implementations.BedTypeServiceImpl`)
This service implementation updates the database using a **Sync List** flow. It updates existing records, adds new records, and deletes records that are no longer present in the client list:
```java
package com.nag.atithi.implementations;

import com.nag.atithi.models.BedType;
import com.nag.atithi.models.Hotel;
import com.nag.atithi.repositories.BedTypeRepository;
import com.nag.atithi.repositories.HotelRepository;
import com.nag.atithi.requests.BedTypeRequest;
import com.nag.atithi.services.BedTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BedTypeServiceImpl implements BedTypeService {

    private final HotelRepository hotelRepository;
    private final BedTypeRepository bedTypeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BedType> getHotelBeds(Long hotelId) {
        return bedTypeRepository.findAllByHotelId(hotelId);
    }

    @Override
    @Transactional
    public void updateHotelBeds(Long hotelId, List<BedTypeRequest> bedsList) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Hotel not found with id: " + hotelId));

        // Fetch currently saved bed types for the hotel
        List<BedType> existingBeds = bedTypeRepository.findAllByHotelId(hotelId);
        Map<Long, BedType> existingBedsMap = existingBeds.stream()
                .collect(Collectors.toMap(BedType::getId, b -> b));

        // Process request items
        Set<Long> idsToKeep = bedsList.stream()
                .filter(req -> req.getId() != null && !req.getId().startsWith("bed-"))
                .map(req -> Long.valueOf(req.getId()))
                .collect(Collectors.toSet());

        // 1. Delete items not present in the new payload list
        List<BedType> bedsToDelete = existingBeds.stream()
                .filter(b -> !idsToKeep.contains(b.getId()))
                .collect(Collectors.toList());
        bedTypeRepository.deleteAll(bedsToDelete);

        // 2. Save new or update existing configurations
        for (BedTypeRequest req : bedsList) {
            BedType bedType;
            if (req.getId() != null && !req.getId().startsWith("bed-")) {
                // Update
                Long id = Long.valueOf(req.getId());
                bedType = existingBedsMap.get(id);
                if (bedType == null) {
                    throw new RuntimeException("Bed type not found for id: " + id);
                }
            } else {
                // Insert New
                bedType = new BedType();
                bedType.setHotel(hotel);
            }

            bedType.setName(req.getName().trim());
            bedType.setDescription(req.getDescription() != null ? req.getDescription().trim() : null);
            bedType.setMaxAdults(req.getMaxAdults());
            bedType.setMaxChildren(req.getMaxChildren());
            bedType.setCapacity(req.getCapacity());

            bedTypeRepository.save(bedType);
        }
    }
}
```

---

## 6. Controller Layer (`com.nag.atithi.controllers.HotelBedsController`)

Implement API endpoints mapping to `@RequestMapping("/api/v1/hotel/beds")`:
```java
package com.nag.atithi.controllers;

import com.nag.atithi.models.BedType;
import com.nag.atithi.models.UserPrincipal;
import com.nag.atithi.requests.UpdateBedsRequest;
import com.nag.atithi.responses.ApiResponse;
import com.nag.atithi.services.BedTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hotel/beds")
@RequiredArgsConstructor
public class HotelBedsController {

    private final BedTypeService bedTypeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BedType>>> getBeds(
            // Spring Security injects the UserPrincipal authenticated context automatically.
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        if (userPrincipal == null || userPrincipal.getHotelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        List<BedType> beds = bedTypeService.getHotelBeds(userPrincipal.getHotelId());
        return ResponseEntity.ok(ApiResponse.success("Hotel beds fetched successfully", beds));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> saveBeds(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdateBedsRequest request
    ) {
        if (userPrincipal == null || userPrincipal.getHotelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        bedTypeService.updateHotelBeds(userPrincipal.getHotelId(), request.getBeds());
        return ResponseEntity.ok(ApiResponse.success("Hotel beds saved successfully", null));
    }
}
```
