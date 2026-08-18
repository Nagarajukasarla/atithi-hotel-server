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
