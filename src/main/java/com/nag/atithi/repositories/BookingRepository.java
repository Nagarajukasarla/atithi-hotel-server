package com.nag.atithi.repositories;

import com.nag.atithi.models.Booking;
import com.nag.atithi.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findAllByHotelIdOrderByIdDesc(Long hotelId);
    Optional<Booking> findFirstByRoomIdAndHotelIdAndStatusNotAndStatusNotOrderByIdDesc(
            Long roomId, Long hotelId, BookingStatus status1, BookingStatus status2
    );
    List<Booking> findByHotelIdAndStatusInAndCheckOutBetween(
            Long hotelId,
            List<BookingStatus> statuses,
            java.time.LocalDateTime start,
            java.time.LocalDateTime end
    );

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.hotel.id = :hotelId " +
           "AND b.status <> :status AND b.checkIn BETWEEN :start AND :end")
    long countTodayCheckIns(
            @Param("hotelId") Long hotelId,
            @Param("status") BookingStatus status,
            @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end
    );

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.hotel.id = :hotelId " +
           "AND b.status <> :status AND b.checkOut BETWEEN :start AND :end")
    long countTodayCheckOuts(
            @Param("hotelId") Long hotelId,
            @Param("status") BookingStatus status,
            @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end
    );

    @Query("SELECT COUNT(g) FROM Booking b JOIN b.guests g WHERE b.hotel.id = :hotelId " +
           "AND b.status = com.nag.atithi.enums.BookingStatus.CHECKED_IN")
    long countTotalGuestsInHotel(@Param("hotelId") Long hotelId);
}
