package com.nag.atithi.responses;

import com.nag.atithi.enums.UserRole;
import com.nag.atithi.enums.UserStatus;
import com.nag.atithi.models.User;

public record UserDto(
        Long id,
        Long hotelId,
        String name,
        String email,
        String phone,
        UserRole role,
        UserStatus status
) {
    public static UserDto fromEntity(User user) {
        return new UserDto(
                user.getId(),
                user.getHotel() != null ? user.getHotel().getId() : null,
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getStatus()
        );
    }
}
