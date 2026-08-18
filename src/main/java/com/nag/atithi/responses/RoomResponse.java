package com.nag.atithi.responses;

import com.nag.atithi.models.Room;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@Builder
public class RoomResponse {
    private String id;
    private String number;
    private String floor;
    private String roomType;
    private Integer capacity;
    private String roomStatus;
    private String roomState;
    private String lastCleaned;
    private String notes;

    public static RoomResponse fromEntity(Room room) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        
        return RoomResponse.builder()
                .id(String.valueOf(room.getId()))
                .number(room.getRoomNumber())
                .floor(room.getFloor())
                .roomType(room.getRoomType() != null ? room.getRoomType().getName() : null)
                .capacity(room.getCapacity())
                .roomStatus(room.getRoomStatus() != null ? room.getRoomStatus().getValue() : null)
                .roomState(room.getRoomState() != null ? room.getRoomState().getValue() : null)
                .lastCleaned(room.getLastCleaned() != null ? room.getLastCleaned().format(formatter) : null)
                .notes(room.getNotes())
                .build();
    }
}
