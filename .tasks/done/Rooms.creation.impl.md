# Room Management — Final Backend Implementation

## 1. Final Domain Structure

The room module contains three primary domain concepts:

```text
Hotel
│
├── RoomType
│   └── Amenities
│
└── Room
```

Example:

```text
Hotel
├── RoomType: Deluxe
│   ├── WiFi
│   ├── AC
│   └── TV
│
├── Room 101 → Deluxe
├── Room 102 → Deluxe
└── Room 103 → Standard
```

### Important Rule

`hotelId` must **never be sent from the frontend**.

The backend derives the hotel from the authenticated user:

```text
JWT
 ↓
Authenticated User
 ↓
User.hotel
 ↓
Hotel
```

This means hotel ownership is always controlled by the backend.

---

## 2. Final Database Tables

The room module uses:

```text
hotels
room_types
rooms
amenities
room_type_amenities
```

The `hotels` table already exists as part of the authentication/domain implementation.

---

## 3. Room Types

A `RoomType` represents a category of rooms.

Examples:

* Standard
* Deluxe
* Executive
* Suite

### Database Schema

```sql
CREATE TABLE room_types (
    id BIGSERIAL PRIMARY KEY,

    hotel_id BIGINT NOT NULL,

    name VARCHAR(100) NOT NULL,
    description TEXT,

    base_price DECIMAL(12, 2) NOT NULL,

    max_adults INTEGER NOT NULL,
    max_children INTEGER DEFAULT 0,
    max_occupancy INTEGER NOT NULL,

    bed_type VARCHAR(50),
    size VARCHAR(50),

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_room_type_hotel
        FOREIGN KEY (hotel_id)
        REFERENCES hotels(id),

    CONSTRAINT uq_room_type_name_per_hotel
        UNIQUE (hotel_id, name)
);
```

### Example

```text
Grand Palace

Standard
├── ₹2500
├── 2 Adults
└── 1 Child

Deluxe
├── ₹3500
├── 2 Adults
└── 2 Children

Suite
├── ₹6000
├── 4 Adults
└── 2 Children
```

---

## 4. Rooms

A `Room` represents the actual physical room.

Example:

```text
Room 101 → Deluxe
Room 102 → Deluxe
Room 103 → Standard
```

### Database Schema

```sql
CREATE TABLE rooms (
    id BIGSERIAL PRIMARY KEY,

    hotel_id BIGINT NOT NULL,
    room_type_id BIGINT NOT NULL,

    room_number VARCHAR(20) NOT NULL,
    floor_number INTEGER,

    description TEXT,

    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_room_hotel
        FOREIGN KEY (hotel_id)
        REFERENCES hotels(id),

    CONSTRAINT fk_room_type
        FOREIGN KEY (room_type_id)
        REFERENCES room_types(id),

    CONSTRAINT uq_room_number_per_hotel
        UNIQUE (hotel_id, room_number)
);
```

### Why Does `rooms` Have Both `hotel_id` and `room_type_id`?

`hotel_id` determines ownership:

```text
Room
 ↓
Hotel
```

`room_type_id` determines the type:

```text
Room
 ↓
RoomType
```

Example:

```text
Room #101
hotel_id = 1
room_type_id = 2
```

means:

```text
Grand Palace
    ↓
Room 101
    ↓
Deluxe Room
```

---

## 5. Room Status

```java
public enum RoomStatus {

    AVAILABLE,
    OCCUPIED,
    RESERVED,
    MAINTENANCE,
    INACTIVE
}
```

| Status        | Meaning                          |
| ------------- | -------------------------------- |
| `AVAILABLE`   | Room can be booked               |
| `OCCUPIED`    | Guest is currently staying       |
| `RESERVED`    | Reserved for an upcoming booking |
| `MAINTENANCE` | Temporarily unavailable          |
| `INACTIVE`    | Room is no longer active         |

### Status Ownership

Eventually, room status should be controlled by operational modules:

```text
Booking
    ↓
RESERVED

Check-in
    ↓
OCCUPIED

Check-out
    ↓
AVAILABLE

Housekeeping
    ↓
MAINTENANCE
```

For the initial room module, basic status management is sufficient.

---

## 6. Amenities

Amenities are reusable/shared features.

Examples:

* Wi-Fi
* Air Conditioning
* TV
* Mini Bar
* Parking
* Breakfast
* Room Service

### Database Schema

```sql
CREATE TABLE amenities (
    id BIGSERIAL PRIMARY KEY,

    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon VARCHAR(100),

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_amenity_name
        UNIQUE (name)
);
```

Amenities are global/shared.

For example, `Wi-Fi` does not need to be created separately for every hotel.

---

## 7. Room Type ↔ Amenity

A room type can have multiple amenities.

Example:

```text
Deluxe
├── Wi-Fi
├── AC
├── TV
└── Mini Bar
```

Therefore, a mapping table is required.

### Database Schema

```sql
CREATE TABLE room_type_amenities (
    room_type_id BIGINT NOT NULL,
    amenity_id BIGINT NOT NULL,

    PRIMARY KEY (room_type_id, amenity_id),

    CONSTRAINT fk_rta_room_type
        FOREIGN KEY (room_type_id)
        REFERENCES room_types(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_rta_amenity
        FOREIGN KEY (amenity_id)
        REFERENCES amenities(id)
        ON DELETE CASCADE
);
```

Relationship:

```text
RoomType * ←→ * Amenity
```

---

# 8. Final Entity Relationship

```text
                         HOTEL
                           │
              ┌────────────┴────────────┐
              │                         │
              ▼                         ▼
         ROOM TYPES                   ROOMS
              │                         │
              │                         │
              ▼                         ▼
   ROOM_TYPE_AMENITIES              ROOM TYPE
              │
              ▼
          AMENITIES
```

More precisely:

```text
Hotel
│
├── RoomType
│   │
│   └── RoomTypeAmenities
│       │
│       └── Amenity
│
└── Room
    │
    └── RoomType
```

---

# 9. Room Type API

A room type must exist before a physical room can be created.

### Create Room Type

```http
POST /api/v1/room-types
```

### Request

```json
{
    "name": "Deluxe",
    "description": "Deluxe room with city view",
    "basePrice": 3500.00,
    "maxAdults": 2,
    "maxChildren": 2,
    "maxOccupancy": 4,
    "bedType": "KING",
    "size": "350 sq.ft"
}
```

### Important

The request does **not** contain:

```json
{
    "hotelId": 1
}
```

The backend obtains the hotel from the authenticated user.

---

# 10. Room Type Controller

```java
@RestController
@RequestMapping("/api/v1/room-types")
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    @PostMapping
    public ResponseEntity<RoomTypeResponse> create(
            @RequestBody CreateRoomTypeRequest request) {

        return ResponseEntity.ok(
            roomTypeService.createRoomType(request)
        );
    }

    @GetMapping
    public ResponseEntity<List<RoomTypeResponse>> getAll() {

        return ResponseEntity.ok(
            roomTypeService.getRoomTypes()
        );
    }
}
```

---

# 11. Room Type Service

### Interface

```java
public interface RoomTypeService {

    RoomTypeResponse createRoomType(
        CreateRoomTypeRequest request
    );

    List<RoomTypeResponse> getRoomTypes();

    RoomTypeResponse getRoomType(Long id);

    RoomTypeResponse updateRoomType(
        Long id,
        UpdateRoomTypeRequest request
    );

    void deleteRoomType(Long id);
}
```

### Implementation Structure

```java
@Service
public class RoomTypeServiceImpl
        implements RoomTypeService {

    @Override
    public RoomTypeResponse createRoomType(
            CreateRoomTypeRequest request) {

        User currentUser = userService.getCurrentUser();

        Hotel hotel = currentUser.getHotel();

        // Validate duplicate room type

        // Create RoomType
        // Set hotel
        // Set pricing
        // Set occupancy
        // Set bed type
        // Save

        return null;
    }
}
```

---

# 12. Room Type Repository

```java
public interface RoomTypeRepository
        extends JpaRepository<RoomType, Long> {

    boolean existsByHotelIdAndName(
        Long hotelId,
        String name
    );

    Optional<RoomType> findByIdAndHotelId(
        Long id,
        Long hotelId
    );

    List<RoomType> findAllByHotelId(
        Long hotelId
    );
}
```

---

# 13. Create Room

Once the hotel has room types such as:

```text
Deluxe
Standard
Suite
```

the owner can create physical rooms.

### Endpoint

```http
POST /api/v1/rooms
```

### Request

```json
{
    "roomNumber": "101",
    "roomTypeId": 2,
    "floorNumber": 1,
    "description": "Deluxe room with city view"
}
```

### Backend-Controlled Fields

The frontend does **not** send:

```text
hotelId ❌
status   ❌
```

The backend determines both.

---

# 14. Create Room Flow

```text
Frontend
    │
    │ POST /api/v1/rooms
    │ JWT
    ▼
RoomController
    │
    ▼
RoomService.createRoom()
    │
    ▼
Authenticated User
    │
    ▼
User.hotel
    │
    ▼
Hotel
    │
    ├── Validate roomTypeId
    │
    ├── Validate room number
    │
    └── Create Room
            │
            ▼
      RoomRepository
            │
            ▼
         Database
```

---

# 15. Room Controller

```java
@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomResponse> create(
            @RequestBody CreateRoomRequest request) {

        return ResponseEntity.ok(
            roomService.createRoom(request)
        );
    }

    @GetMapping
    public ResponseEntity<List<RoomResponse>> getAll() {

        return ResponseEntity.ok(
            roomService.getRooms()
        );
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> get(
            @PathVariable Long roomId) {

        return ResponseEntity.ok(
            roomService.getRoom(roomId)
        );
    }

    @PutMapping("/{roomId}")
    public ResponseEntity<RoomResponse> update(
            @PathVariable Long roomId,
            @RequestBody UpdateRoomRequest request) {

        return ResponseEntity.ok(
            roomService.updateRoom(roomId, request)
        );
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long roomId) {

        roomService.deleteRoom(roomId);

        return ResponseEntity.noContent().build();
    }
}
```

---

# 16. Room Service

```java
public interface RoomService {

    RoomResponse createRoom(
        CreateRoomRequest request
    );

    List<RoomResponse> getRooms();

    RoomResponse getRoom(
        Long roomId
    );

    RoomResponse updateRoom(
        Long roomId,
        UpdateRoomRequest request
    );

    void deleteRoom(
        Long roomId
    );
}
```

---

# 17. Room Service Implementation

```java
@Service
public class RoomServiceImpl
        implements RoomService {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final UserService userService;

    @Override
    public RoomResponse createRoom(
            CreateRoomRequest request) {

        User user = userService.getCurrentUser();

        Hotel hotel = user.getHotel();

        // Check room number
        // Check room type belongs to this hotel
        // Create Room
        // Set hotel
        // Set room type
        // Set room number
        // Set floor
        // Set description
        // Set AVAILABLE status

        // Save room

        return null;
    }
}
```

### Required Validations

The service must validate:

1. User is authenticated.
2. Hotel exists.
3. Room number is unique within the hotel.
4. Room type belongs to the same hotel.

The fourth validation prevents cross-hotel references:

```text
Hotel A
   ↓
Room
   ↓
RoomType belonging to Hotel B ❌
```

---

# 18. Room Repository

```java
public interface RoomRepository
        extends JpaRepository<Room, Long> {

    boolean existsByHotelIdAndRoomNumber(
        Long hotelId,
        String roomNumber
    );

    Optional<Room> findByIdAndHotelId(
        Long roomId,
        Long hotelId
    );

    List<Room> findAllByHotelIdAndStatus(
        Long hotelId,
        RoomStatus status
    );

    List<Room> findAllByHotelId(
        Long hotelId
    );
}
```

---

# 19. Update Room

### Endpoint

```http
PUT /api/v1/rooms/{roomId}
```

### Request

```json
{
    "roomNumber": "101",
    "roomTypeId": 3,
    "floorNumber": 1,
    "description": "Updated room description"
}
```

### Flow

```text
PUT /rooms/10
    ↓
RoomController
    ↓
RoomService.updateRoom()
    ↓
Current User
    ↓
Current Hotel
    ↓
Find Room using roomId + hotelId
    ↓
Validate new RoomType
    ↓
Update
    ↓
Save
```

### Service Structure

```java
@Override
public RoomResponse updateRoom(
        Long roomId,
        UpdateRoomRequest request) {

    User user = userService.getCurrentUser();

    Long hotelId = user.getHotel().getId();

    Room room = roomRepository
        .findByIdAndHotelId(roomId, hotelId)
        .orElseThrow(() ->
            new RuntimeException("Room not found")
        );

    // Validate room type belongs to hotel

    // Update fields

    return null;
}
```

---

# 20. Delete Room

Room deletion should use **soft delete**.

The database record remains because historical bookings may reference the room.

### Endpoint

```http
DELETE /api/v1/rooms/{roomId}
```

### Flow

```text
DELETE /rooms/10
    ↓
Find Room
    ↓
Verify hotel ownership
    ↓
status = INACTIVE
    ↓
Save
```

### Service

```java
@Override
public void deleteRoom(Long roomId) {

    User user = userService.getCurrentUser();

    Long hotelId = user.getHotel().getId();

    Room room = roomRepository
        .findByIdAndHotelId(roomId, hotelId)
        .orElseThrow(() ->
            new RuntimeException("Room not found")
        );

    room.setStatus(RoomStatus.INACTIVE);

    roomRepository.save(room);
}
```

Therefore:

```http
DELETE /rooms/10
```

does not physically delete the database row.

Instead:

```text
AVAILABLE
    ↓
INACTIVE
```

---

# 21. Room Status Updates

Room profile information:

```http
PUT /api/v1/rooms/{roomId}
```

Operational status should eventually have a separate endpoint:

```http
PATCH /api/v1/rooms/{roomId}/status
```

### Request

```json
{
    "status": "MAINTENANCE"
}
```

This separation allows operational services to control room availability independently.

Eventually:

```text
Booking
    ↓
RESERVED

Check-in
    ↓
OCCUPIED

Check-out
    ↓
AVAILABLE

Housekeeping
    ↓
MAINTENANCE
```

---

# 22. Frontend → Backend Flow

## Create Room

```text
Rooms Page
    ↓
Add Room
    ↓
Select Room Type
    ↓
Enter Room Number
    ↓
Enter Floor
    ↓
Enter Description
    ↓
Submit
    ↓
POST /api/v1/rooms
    ↓
RoomController
    ↓
RoomService
    ↓
PostgreSQL
    ↓
Success
    ↓
Refresh Rooms
```

### Frontend Request Body

```json
{
    "roomNumber": "101",
    "roomTypeId": 2,
    "floorNumber": 1,
    "description": "Deluxe room"
}
```

## Update Room

```text
Rooms Page
    ↓
Edit Room
    ↓
Change details
    ↓
PUT /api/v1/rooms/101
    ↓
RoomController
    ↓
RoomService
    ↓
Database UPDATE
    ↓
Updated Room
```

## Delete Room

```text
Rooms Page
    ↓
Delete Room
    ↓
DELETE /api/v1/rooms/101
    ↓
RoomService
    ↓
status = INACTIVE
    ↓
Database
    ↓
Room removed from active list
```

---

# 23. Final API List

## Room Types

| Method   | Endpoint                  |
| -------- | ------------------------- |
| `POST`   | `/api/v1/room-types`      |
| `GET`    | `/api/v1/room-types`      |
| `GET`    | `/api/v1/room-types/{id}` |
| `PUT`    | `/api/v1/room-types/{id}` |
| `DELETE` | `/api/v1/room-types/{id}` |

## Rooms

| Method   | Endpoint                        |
| -------- | ------------------------------- |
| `POST`   | `/api/v1/rooms`                 |
| `GET`    | `/api/v1/rooms`                 |
| `GET`    | `/api/v1/rooms/{roomId}`        |
| `PUT`    | `/api/v1/rooms/{roomId}`        |
| `DELETE` | `/api/v1/rooms/{roomId}`        |
| `PATCH`  | `/api/v1/rooms/{roomId}/status` |

## Amenities

| Method   | Endpoint                 |
| -------- | ------------------------ |
| `POST`   | `/api/v1/amenities`      |
| `GET`    | `/api/v1/amenities`      |
| `PUT`    | `/api/v1/amenities/{id}` |
| `DELETE` | `/api/v1/amenities/{id}` |

## Room Type Amenities

```http
PUT /api/v1/room-types/{roomTypeId}/amenities
```

Example:

```json
{
    "amenityIds": [1, 2, 4, 7]
}
```

---

# 24. Final Data Model

```text
                    GRAND PALACE HOTEL
                           │
              ┌────────────┴────────────┐
              │                         │
              ▼                         ▼
         ROOM TYPES                   ROOMS
              │                         │
              │                         │
              ▼                         ▼
     ROOM_TYPE_AMENITIES             ROOM TYPE
              │
              ▼
          AMENITIES
```

Example:

```text
Grand Palace Hotel
│
├── Room Types
│   │
│   ├── Standard ₹2500
│   │   ├── Wi-Fi
│   │   └── AC
│   │
│   └── Deluxe ₹3500
│       ├── Wi-Fi
│       ├── AC
│       ├── TV
│       └── Mini Bar
│
└── Rooms
    │
    ├── 101 → Deluxe → AVAILABLE
    ├── 102 → Deluxe → OCCUPIED
    ├── 103 → Standard → AVAILABLE
    └── 104 → Standard → MAINTENANCE
```

---

# Final Rule

The ownership and feature structure for the room module is:

```text
Hotel
  │
  ├── RoomType
  │     │
  │     └── Amenity
  │
  └── Room
        │
        └── RoomType
```

### Core Principle

**Hotel → RoomType → Room** defines ownership and room classification.

**RoomType ↔ Amenity** defines reusable room features.

The frontend only sends room and room-type information.

The backend derives `hotelId` from the authenticated user.

```text
Frontend
    │
    │ Room / RoomType information
    ▼
Backend
    │
    │ JWT
    ▼
Authenticated User
    │
    ▼
User.hotel
    │
    ▼
Hotel
```

This keeps hotel ownership server-controlled and prevents users from creating or accessing rooms belonging to another hotel.
