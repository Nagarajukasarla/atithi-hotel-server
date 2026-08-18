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
