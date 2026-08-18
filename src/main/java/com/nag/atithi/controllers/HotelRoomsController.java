package com.nag.atithi.controllers;

import com.nag.atithi.models.Room;
import com.nag.atithi.models.UserPrincipal;
import com.nag.atithi.requests.CheckAvailabilityRequest;
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
}
