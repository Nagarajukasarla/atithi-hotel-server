package com.nag.atithi.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRoomRequest {

    @NotBlank(message = "Room number is required")
    private String number;

    @NotBlank(message = "Floor position is required")
    private String floor;

    @NotBlank(message = "Room category/type is required")
    private String roomType;

    @NotNull(message = "Maximum capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1 guest")
    private Integer capacity;

    private String notes;
}
