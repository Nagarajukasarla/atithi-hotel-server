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
