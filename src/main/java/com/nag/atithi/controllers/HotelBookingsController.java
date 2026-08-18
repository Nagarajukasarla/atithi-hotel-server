package com.nag.atithi.controllers;

import com.nag.atithi.models.UserPrincipal;
import com.nag.atithi.requests.CreateBookingRequest;
import com.nag.atithi.requests.UpdateBookingRequest;
import com.nag.atithi.requests.PaymentRequest;
import com.nag.atithi.responses.ApiResponse;
import com.nag.atithi.responses.BookingResponse;
import com.nag.atithi.responses.PaymentResponse;
import com.nag.atithi.services.BookingService;
import com.nag.atithi.services.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/hotel/bookings")
@RequiredArgsConstructor
public class HotelBookingsController {

    private final BookingService bookingService;
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateBookingRequest request
    ) {
        if (userPrincipal == null || userPrincipal.getHotelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        BookingResponse bookingResponse = bookingService.createBooking(userPrincipal.getHotelId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created successfully", bookingResponse));
    }

    @PostMapping("/{bookingId}/payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> createOrUpdatePayment(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long bookingId,
            @Valid @RequestBody PaymentRequest request
    ) {
        if (userPrincipal == null || userPrincipal.getHotelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        PaymentResponse paymentResponse = paymentService.createOrUpdatePayment(bookingId, request);
        return ResponseEntity.ok(ApiResponse.success("Payment details saved successfully", paymentResponse));
    }

    @GetMapping("/{bookingId}/payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long bookingId
    ) {
        if (userPrincipal == null || userPrincipal.getHotelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        PaymentResponse paymentResponse = paymentService.getPaymentByBookingId(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Payment details fetched successfully", paymentResponse));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<BookingResponse>> getActiveBooking(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam Long roomId
    ) {
        if (userPrincipal == null || userPrincipal.getHotelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        BookingResponse bookingResponse = bookingService.getActiveBookingForRoom(userPrincipal.getHotelId(), roomId);
        return ResponseEntity.ok(ApiResponse.success("Active booking details fetched successfully", bookingResponse));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getAllBookings(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        if (userPrincipal == null || userPrincipal.getHotelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        List<BookingResponse> bookings = bookingService.getAllBookings(userPrincipal.getHotelId());
        return ResponseEntity.ok(ApiResponse.success("Bookings fetched successfully", bookings));
    }

    @PutMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> updateBooking(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long bookingId,
            @Valid @RequestBody UpdateBookingRequest request
    ) {
        if (userPrincipal == null || userPrincipal.getHotelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        BookingResponse bookingResponse = bookingService.updateBooking(userPrincipal.getHotelId(), bookingId, request);
        return ResponseEntity.ok(ApiResponse.success("Booking updated successfully", bookingResponse));
    }

    @PostMapping("/{bookingId}/check-in")
    public ResponseEntity<ApiResponse<BookingResponse>> checkInBooking(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long bookingId
    ) {
        if (userPrincipal == null || userPrincipal.getHotelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        BookingResponse bookingResponse = bookingService.checkInBooking(userPrincipal.getHotelId(), bookingId);
        return ResponseEntity.ok(ApiResponse.success("Booking checked in successfully", bookingResponse));
    }

    @PostMapping("/{bookingId}/checkout")
    public ResponseEntity<ApiResponse<BookingResponse>> checkOutBooking(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long bookingId
    ) {
        if (userPrincipal == null || userPrincipal.getHotelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        BookingResponse bookingResponse = bookingService.checkOutBooking(userPrincipal.getHotelId(), bookingId);
        return ResponseEntity.ok(ApiResponse.success("Booking checked out successfully", bookingResponse));
    }

    @GetMapping("/checking-out-soon")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsCheckingOutSoon(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        if (userPrincipal == null || userPrincipal.getHotelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        List<BookingResponse> bookings = bookingService.getBookingsCheckingOutSoon(userPrincipal.getHotelId());
        return ResponseEntity.ok(ApiResponse.success("Upcoming checkouts fetched successfully", bookings));
    }
}
