package com.nag.atithi.services;

import com.nag.atithi.requests.CreateBookingRequest;
import com.nag.atithi.requests.UpdateBookingRequest;
import com.nag.atithi.responses.BookingResponse;
import java.util.List;

public interface BookingService {
    BookingResponse createBooking(Long hotelId, CreateBookingRequest request);
    BookingResponse getActiveBookingForRoom(Long hotelId, Long roomId);
    List<BookingResponse> getAllBookings(Long hotelId);
    BookingResponse updateBooking(Long hotelId, Long bookingId, UpdateBookingRequest request);
    BookingResponse checkInBooking(Long hotelId, Long bookingId);
    BookingResponse checkOutBooking(Long hotelId, Long bookingId);
    List<BookingResponse> getBookingsCheckingOutSoon(Long hotelId);
}
