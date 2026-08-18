package com.nag.atithi.responses;

import com.nag.atithi.models.Booking;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
public class BookingResponse {
    private Long id;
    private Long hotelId;
    private Long roomId;
    private String roomNumber;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private LocalDateTime actualCheckOut;
    private Boolean isHourly;
    private Double totalPrice;
    private String status;
    private List<BookingGuestResponse> guests;
    private PaymentResponse payment;

    public static BookingResponse fromEntity(Booking booking) {
        if (booking == null) return null;
        return BookingResponse.builder()
                .id(booking.getId())
                .hotelId(booking.getHotel() != null ? booking.getHotel().getId() : null)
                .roomId(booking.getRoom() != null ? booking.getRoom().getId() : null)
                .roomNumber(booking.getRoom() != null ? booking.getRoom().getRoomNumber() : null)
                .checkIn(booking.getCheckIn())
                .checkOut(booking.getCheckOut())
                .actualCheckOut(booking.getActualCheckOut())
                .isHourly(booking.getIsHourly())
                .totalPrice(booking.getTotalPrice())
                .status(booking.getStatus() != null ? booking.getStatus().name() : null)
                .guests(booking.getGuests() != null ? booking.getGuests().stream()
                        .map(BookingGuestResponse::fromEntity)
                        .collect(Collectors.toList()) : List.of())
                .payment(booking.getPayment() != null ? PaymentResponse.fromEntity(booking.getPayment()) : null)
                .build();
    }
}
