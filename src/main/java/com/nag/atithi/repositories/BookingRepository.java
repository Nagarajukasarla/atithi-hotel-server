package com.nag.atithi.repositories;

import com.nag.atithi.models.Booking;
import com.nag.atithi.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
