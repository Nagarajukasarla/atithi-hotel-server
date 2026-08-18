package com.nag.atithi.implementations;

import com.nag.atithi.enums.BookingStatus;
import com.nag.atithi.enums.RoomStatus;
import com.nag.atithi.enums.RoomState;
import java.time.LocalDateTime;
import com.nag.atithi.models.*;
import com.nag.atithi.repositories.*;
import com.nag.atithi.requests.CreateBookingRequest;
import com.nag.atithi.requests.UpdateBookingRequest;
import com.nag.atithi.responses.BookingResponse;
import com.nag.atithi.services.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;

    @Override
    @Transactional
    public BookingResponse createBooking(Long hotelId, CreateBookingRequest request) {
        // 1. Fetch Hotel
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new IllegalArgumentException("Hotel not found with id: " + hotelId));

        // 2. Fetch Room (using findByIdAndHotelId to verify ownership)
        Room room = roomRepository.findByIdAndHotelId(request.getRoomId(), hotelId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + request.getRoomId()));

        // 3. Business Rule Validation: Room status must be VACANT
        if (room.getRoomStatus() != RoomStatus.VACANT) {
            throw new IllegalStateException("Room #" + room.getRoomNumber() + " is currently " + room.getRoomStatus() + " (not Vacant).");
        }

        // 4. Calculate total booking price based on RoomType basePrice configuration
        Double totalPrice = calculateTotalPrice(room, request);

        // 5. Build Booking Entity
        Booking booking = Booking.builder()
                .hotel(hotel)
                .room(room)
                .checkIn(request.getCheckIn())
                .checkOut(request.getCheckOut())
                .isHourly(request.getIsHourly())
                .totalPrice(totalPrice)
                .status(BookingStatus.CONFIRMED)
                .build();

        // 6. Map Guest Requests to Entities
        List<BookingGuest> guests = request.getGuests().stream()
                .map(gRequest -> BookingGuest.builder()
                        .booking(booking)
                        .name(gRequest.getName().trim())
                        .aadhaarNumber(gRequest.getAadhaarNumber().trim())
                        .mobileNumber(gRequest.getMobileNumber().trim())
                        .age(gRequest.getAge())
                        .address(gRequest.getAddress() != null ? gRequest.getAddress().trim() : null)
                        .build())
                .collect(Collectors.toList());

        booking.setGuests(guests);

        // 7. Update Room Status from VACANT to OCCUPIED
        room.setRoomStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room); // Saves room status change

        // 8. Save Booking (CascadeType.ALL will automatically save the mapped guests list)
        Booking savedBooking = bookingRepository.save(booking);

        return BookingResponse.fromEntity(savedBooking);
    }

    private Double calculateTotalPrice(Room room, CreateBookingRequest request) {
        Duration duration = Duration.between(request.getCheckIn(), request.getCheckOut());
        long diffSeconds = duration.getSeconds();
        if (diffSeconds <= 0) {
            throw new IllegalArgumentException("Check-out time must be after check-in time.");
        }

        if (room.getRoomType() == null || room.getRoomType().getBasePrice() == null) {
            throw new IllegalStateException("Room type or base price is not defined for this room.");
        }

        double basePrice = room.getRoomType().getBasePrice().doubleValue();

        if (request.getIsHourly()) {
            double hours = Math.ceil(diffSeconds / 3600.0);
            double hourlyRate = Math.round(basePrice / 10.0);
            return hours * hourlyRate;
        } else {
            double days = Math.ceil(diffSeconds / 86400.0);
            return days * basePrice;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getActiveBookingForRoom(Long hotelId, Long roomId) {
        Booking booking = bookingRepository.findFirstByRoomIdAndHotelIdAndStatusNotAndStatusNotOrderByIdDesc(
                roomId, hotelId, BookingStatus.CANCELLED, BookingStatus.CHECKED_OUT
        ).orElseThrow(() -> new IllegalArgumentException("No active booking found for room id: " + roomId));
        return BookingResponse.fromEntity(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings(Long hotelId) {
        List<Booking> bookings = bookingRepository.findAllByHotelIdOrderByIdDesc(hotelId);
        return bookings.stream()
                .map(BookingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BookingResponse updateBooking(Long hotelId, Long bookingId, UpdateBookingRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));
        
        if (!booking.getHotel().getId().equals(hotelId)) {
            throw new IllegalArgumentException("Booking does not belong to this hotel.");
        }

        // Update fields
        booking.setTotalPrice(request.getTotalPrice());
        booking.setCheckIn(request.getCheckIn());
        booking.setCheckOut(request.getCheckOut());
        booking.setIsHourly(request.getIsHourly());

        // Update guests by clearing and adding new ones
        booking.getGuests().clear();
        
        List<BookingGuest> updatedGuests = request.getGuests().stream()
                .map(gRequest -> BookingGuest.builder()
                        .booking(booking)
                        .name(gRequest.getName().trim())
                        .aadhaarNumber(gRequest.getAadhaarNumber().trim())
                        .mobileNumber(gRequest.getMobileNumber().trim())
                        .age(gRequest.getAge())
                        .address(gRequest.getAddress() != null ? gRequest.getAddress().trim() : null)
                        .build())
                .collect(Collectors.toList());

        booking.getGuests().addAll(updatedGuests);

        Booking savedBooking = bookingRepository.save(booking);
        return BookingResponse.fromEntity(savedBooking);
    }

    @Override
    @Transactional
    public BookingResponse checkInBooking(Long hotelId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        if (!booking.getHotel().getId().equals(hotelId)) {
            throw new IllegalArgumentException("Booking does not belong to this hotel.");
        }

        if (booking.getStatus() == BookingStatus.CHECKED_OUT || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Cannot check-in booking in status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CHECKED_IN);
        Room room = booking.getRoom();
        room.setRoomStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);

        Booking savedBooking = bookingRepository.save(booking);
        return BookingResponse.fromEntity(savedBooking);
    }

    @Override
    @Transactional
    public BookingResponse checkOutBooking(Long hotelId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        if (!booking.getHotel().getId().equals(hotelId)) {
            throw new IllegalArgumentException("Booking does not belong to this hotel.");
        }

        if (booking.getStatus() == BookingStatus.CHECKED_OUT || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CHECKED_OUT);
        booking.setActualCheckOut(LocalDateTime.now());
        Room room = booking.getRoom();
        room.setRoomStatus(RoomStatus.VACANT);
        room.setRoomState(RoomState.DIRTY);
        roomRepository.save(room);

        Booking savedBooking = bookingRepository.save(booking);
        return BookingResponse.fromEntity(savedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsCheckingOutSoon(Long hotelId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourLater = now.plusHours(1);
        
        List<BookingStatus> activeStatuses = List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN);
        List<Booking> bookings = bookingRepository.findByHotelIdAndStatusInAndCheckOutBetween(
                hotelId, activeStatuses, now, oneHourLater
        );

        return bookings.stream()
                .map(BookingResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
