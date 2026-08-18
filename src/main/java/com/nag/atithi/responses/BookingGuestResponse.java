package com.nag.atithi.responses;

import com.nag.atithi.models.BookingGuest;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BookingGuestResponse {
    private Long id;
    private String name;
    private String aadhaarNumber;
    private String mobileNumber;
    private Integer age;
    private String address;

    public static BookingGuestResponse fromEntity(BookingGuest guest) {
        if (guest == null) return null;
        return BookingGuestResponse.builder()
                .id(guest.getId())
                .name(guest.getName())
                .aadhaarNumber(guest.getAadhaarNumber())
                .mobileNumber(guest.getMobileNumber())
                .age(guest.getAge())
                .address(guest.getAddress())
                .build();
    }
}
