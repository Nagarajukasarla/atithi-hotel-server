package com.nag.atithi.services;

import com.nag.atithi.models.Room;
import com.nag.atithi.requests.CheckAvailabilityRequest;
import com.nag.atithi.requests.CreateRoomRequest;
import com.nag.atithi.requests.UpdateRoomRequest;

import java.util.List;

public interface RoomService {
    List<Room> getHotelRooms(Long hotelId);
    Room createRoom(Long hotelId, CreateRoomRequest request);
    Room updateRoom(Long hotelId, Long roomId, UpdateRoomRequest request);
    void deleteRoom(Long hotelId, Long roomId);
    List<Room> checkAvailability(Long hotelId, CheckAvailabilityRequest request);
}
