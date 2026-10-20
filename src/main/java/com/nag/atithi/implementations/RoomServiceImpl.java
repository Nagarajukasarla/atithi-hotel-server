package com.nag.atithi.implementations;

import com.nag.atithi.enums.RoomState;
import com.nag.atithi.enums.RoomStatus;
import com.nag.atithi.enums.BookingStatus;
import com.nag.atithi.models.Hotel;
import com.nag.atithi.models.Room;
import com.nag.atithi.models.RoomType;
import com.nag.atithi.repositories.HotelRepository;
import com.nag.atithi.repositories.RoomRepository;
import com.nag.atithi.repositories.RoomTypeRepository;
import com.nag.atithi.repositories.BookingRepository;
import com.nag.atithi.requests.CheckAvailabilityRequest;
import com.nag.atithi.requests.CreateRoomRequest;
import com.nag.atithi.requests.UpdateRoomRequest;
import com.nag.atithi.responses.OverviewStatsResponse;
import com.nag.atithi.services.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Room> getHotelRooms(Long hotelId) {
        return roomRepository.findAllByHotelIdOrderByRoomNumberAsc(hotelId);
    }

    @Override
    @Transactional
    public Room createRoom(Long hotelId, CreateRoomRequest request) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Hotel not found with id: " + hotelId));

        String roomNumber = request.getNumber().trim();
        // Validation: Unique room number per hotel
        if (roomRepository.existsByHotelIdAndRoomNumber(hotelId, roomNumber)) {
            throw new IllegalArgumentException("Room number '" + roomNumber + "' already exists in this hotel.");
        }

        // Fetch RoomType config by name
        RoomType roomType = roomTypeRepository.findByHotelIdAndName(hotelId, request.getRoomType().trim())
                .orElseThrow(() -> new RuntimeException("Room category '" + request.getRoomType() + "' not found."));

        Room room = Room.builder()
                .hotel(hotel)
                .roomNumber(roomNumber)
                .floor(request.getFloor().trim())
                .roomType(roomType)
                .capacity(request.getCapacity())
                .roomStatus(RoomStatus.VACANT)
                .roomState(RoomState.CLEAN)
                .lastCleaned(LocalDateTime.now())
                .notes(request.getNotes() != null ? request.getNotes().trim() : null)
                .build();

        return roomRepository.save(room);
    }

    @Override
    @Transactional
    public Room updateRoom(Long hotelId, Long roomId, UpdateRoomRequest request) {
        Room room = roomRepository.findByIdAndHotelId(roomId, hotelId)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + roomId));

        // Validation: room number uniqueness check upon change
        if (request.getNumber() != null && !request.getNumber().trim().isEmpty()) {
            String newNumber = request.getNumber().trim();
            if (!newNumber.equalsIgnoreCase(room.getRoomNumber()) &&
                    roomRepository.existsByHotelIdAndRoomNumber(hotelId, newNumber)) {
                throw new IllegalArgumentException("Room number '" + newNumber + "' already exists in this hotel.");
            }
            room.setRoomNumber(newNumber);
        }

        if (request.getFloor() != null) {
            room.setFloor(request.getFloor().trim());
        }

        if (request.getRoomType() != null && !request.getRoomType().trim().isEmpty()) {
            RoomType roomType = roomTypeRepository.findByHotelIdAndName(hotelId, request.getRoomType().trim())
                    .orElseThrow(() -> new RuntimeException("Room category '" + request.getRoomType() + "' not found."));
            room.setRoomType(roomType);
        }

        if (request.getCapacity() != null) {
            room.setCapacity(request.getCapacity());
        }

        if (request.getRoomStatus() != null) {
            room.setRoomStatus(request.getRoomStatus());
        }

        if (request.getRoomState() != null) {
            room.setRoomState(request.getRoomState());
            // Business rule: automatically update timestamp when marked clean
            if (request.getRoomState() == RoomState.CLEAN) {
                room.setLastCleaned(LocalDateTime.now());
            }
        }

        if (request.getNotes() != null) {
            room.setNotes(request.getNotes().trim());
        }

        return roomRepository.save(room);
    }

    @Override
    @Transactional
    public void deleteRoom(Long hotelId, Long roomId) {
        Room room = roomRepository.findByIdAndHotelId(roomId, hotelId)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + roomId));
        roomRepository.delete(room);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Room> checkAvailability(Long hotelId, CheckAvailabilityRequest request) {
        RoomStatus status = RoomStatus.fromValue(request.getStatus().trim());
        return roomRepository.findAvailableRooms(
            hotelId,
            request.getRoomType().trim(),
            request.getMaxOccupancy(),
            status
        );
    }

    @Override
    @Transactional(readOnly = true)
    public OverviewStatsResponse getOverviewStats(Long hotelId) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);

        long checkIns = bookingRepository.countTodayCheckIns(
            hotelId, BookingStatus.CANCELLED, start, end
        );

        long checkOuts = bookingRepository.countTodayCheckOuts(
            hotelId, BookingStatus.CANCELLED, start, end
        );

        long totalGuests = bookingRepository.countTotalGuestsInHotel(hotelId);

        long availableRooms = roomRepository.countByHotelIdAndRoomStatus(hotelId, RoomStatus.VACANT);

        return new OverviewStatsResponse(checkIns, checkOuts, totalGuests, availableRooms);
    }
}
