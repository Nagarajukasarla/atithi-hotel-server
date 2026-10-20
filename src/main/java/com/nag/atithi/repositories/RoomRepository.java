package com.nag.atithi.repositories;

import com.nag.atithi.enums.RoomStatus;
import com.nag.atithi.models.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findAllByHotelIdOrderByRoomNumberAsc(Long hotelId);
    boolean existsByHotelIdAndRoomNumber(Long hotelId, String roomNumber);
    Optional<Room> findByIdAndHotelId(Long id, Long hotelId);

    @Query("SELECT r.roomType.id, COUNT(r), " +
           "SUM(CASE WHEN r.roomStatus <> :vacantStatus THEN 1 ELSE 0 END) " +
           "FROM Room r WHERE r.hotel.id = :hotelId " +
           "GROUP BY r.roomType.id")
    List<Object[]> countRoomsByRoomTypeAndStatus(
        @Param("hotelId") Long hotelId,
        @Param("vacantStatus") RoomStatus vacantStatus
    );

    @Query("SELECT r FROM Room r JOIN FETCH r.roomType rt WHERE r.hotel.id = :hotelId " +
           "AND ((:roomTypeName = 'Premium' AND rt.isPremium = true) OR rt.name = :roomTypeName) " +
           "AND r.capacity >= :capacity AND r.roomStatus = :roomStatus")
    List<Room> findAvailableRooms(
        @Param("hotelId") Long hotelId,
        @Param("roomTypeName") String roomTypeName,
        @Param("capacity") Integer capacity,
        @Param("roomStatus") RoomStatus roomStatus
    );

    long countByHotelIdAndRoomStatus(Long hotelId, RoomStatus roomStatus);
}
