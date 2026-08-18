package com.nag.atithi.responses;

import com.nag.atithi.enums.HotelStatus;
import com.nag.atithi.models.Hotel;

public record HotelDto(
        Long id,
        String name,
        String phone,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String country,
        String postalCode,
        HotelStatus status
) {
    public static HotelDto fromEntity(Hotel hotel) {
        return new HotelDto(
                hotel.getId(),
                hotel.getName(),
                hotel.getPhone(),
                hotel.getAddressLine1(),
                hotel.getAddressLine2(),
                hotel.getCity(),
                hotel.getState(),
                hotel.getCountry(),
                hotel.getPostalCode(),
                hotel.getStatus()
        );
    }
}
