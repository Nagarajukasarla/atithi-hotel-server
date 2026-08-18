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
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        if (userPrincipal == null || userPrincipal.getHotelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        List<String> selectedCodes = amenityService.getHotelAmenities(userPrincipal.getHotelId());
        return ResponseEntity.ok(ApiResponse.success("Hotel amenities fetched successfully", selectedCodes));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> saveAmenities(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
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
