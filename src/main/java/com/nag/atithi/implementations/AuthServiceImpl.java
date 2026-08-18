package com.nag.atithi.implementations;

import com.nag.atithi.enums.HotelStatus;
import com.nag.atithi.enums.UserRole;
import com.nag.atithi.enums.UserStatus;
import com.nag.atithi.models.Hotel;
import com.nag.atithi.models.User;
import com.nag.atithi.repositories.HotelRepository;
import com.nag.atithi.repositories.UserRepository;
import com.nag.atithi.requests.LoginRequest;
import com.nag.atithi.requests.SignupRequest;
import com.nag.atithi.responses.AuthResponse;
import com.nag.atithi.responses.HotelDto;
import com.nag.atithi.responses.UserDto;
import com.nag.atithi.security.JwtService;
import com.nag.atithi.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("User already exists with email: " + request.email());
        }

        // 1. Create Hotel
        Hotel hotel = Hotel.builder()
                .name(request.hotelName())
                .phone(request.phone())
                .city(request.city())
                .state(request.state())
                .country(request.country())
                .postalCode(request.postalCode())
                .status(HotelStatus.ACTIVE)
                .build();
        Hotel savedHotel = hotelRepository.save(hotel);

        // 2. Create User (Owner)
        User user = User.builder()
                .hotel(savedHotel)
                .name(request.ownerName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .role(UserRole.OWNER)
                .status(UserStatus.ACTIVE)
                .build();
        User savedUser = userRepository.save(user);

        // 3. Generate JWT Token
        String token = jwtService.generateToken(savedUser);

        return new AuthResponse(
                "Signup successful",
                token,
                UserDto.fromEntity(savedUser),
                HotelDto.fromEntity(savedHotel)
        );
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        String token = jwtService.generateToken(user);

        return new AuthResponse(
                "Login successful",
                token,
                UserDto.fromEntity(user),
                HotelDto.fromEntity(user.getHotel())
        );
    }
}
