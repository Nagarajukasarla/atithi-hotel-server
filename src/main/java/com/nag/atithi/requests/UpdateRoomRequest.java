package com.nag.atithi.requests;

import com.nag.atithi.enums.RoomState;
import com.nag.atithi.enums.RoomStatus;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRoomRequest {

    private String number;

    private String floor;

    private String roomType;

    @Min(value = 1, message = "Capacity must be at least 1 guest")
    private Integer capacity;

    private RoomStatus roomStatus;

    private RoomState roomState;

    private String notes;
}
