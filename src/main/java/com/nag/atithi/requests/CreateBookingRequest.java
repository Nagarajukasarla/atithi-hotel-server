package com.nag.atithi.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateBookingRequest {
    @NotNull(message = "Room ID is required")
    private Long roomId;

    @NotNull(message = "Check-in time is required")
    private LocalDateTime checkIn;

    @NotNull(message = "Check-out time is required")
    private LocalDateTime checkOut;

    @NotNull(message = "isHourly flag is required")
    private Boolean isHourly;

    @NotEmpty(message = "At least one guest detail is required")
    @Valid
    private List<GuestRequest> guests;
}
