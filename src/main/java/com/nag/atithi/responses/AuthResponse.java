package com.nag.atithi.responses;

public record AuthResponse(
        String message,
        String token,
        UserDto user,
        HotelDto hotel
) {}
