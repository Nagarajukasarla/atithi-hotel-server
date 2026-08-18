package com.nag.atithi.services;

import com.nag.atithi.requests.RoomTypeRequest;
import com.nag.atithi.responses.RoomTypeResponse;

import java.util.List;

public interface RoomTypeService {
    List<RoomTypeResponse> getHotelRoomTypes(Long hotelId);
    RoomTypeResponse createRoomType(Long hotelId, RoomTypeRequest request);
    RoomTypeResponse updateRoomType(Long hotelId, Long id, RoomTypeRequest request);
    void deleteRoomType(Long hotelId, Long id);
}
