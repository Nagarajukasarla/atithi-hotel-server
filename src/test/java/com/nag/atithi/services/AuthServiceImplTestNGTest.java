package com.nag.atithi.services;

import com.nag.atithi.enums.HotelStatus;
import com.nag.atithi.enums.UserRole;
import com.nag.atithi.enums.UserStatus;
import com.nag.atithi.implementations.AuthServiceImpl;
import com.nag.atithi.models.Hotel;
import com.nag.atithi.models.User;
import com.nag.atithi.repositories.HotelRepository;
import com.nag.atithi.repositories.UserRepository;
import com.nag.atithi.requests.LoginRequest;
import com.nag.atithi.requests.SignupRequest;
import com.nag.atithi.responses.AuthResponse;
import com.nag.atithi.security.JwtService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AuthServiceImplTestNGTest {

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private AutoCloseable mocks;
    private SignupRequest signupRequest;
    private LoginRequest loginRequest;
    private Hotel sampleHotel;
    private User sampleUser;

    @BeforeClass
    public void setUpClass() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterClass
    public void tearDownClass() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @BeforeMethod
    public void setUp() {
        Mockito.reset(hotelRepository, userRepository, passwordEncoder, jwtService, authenticationManager);

        signupRequest = new SignupRequest(
                "Grand Palace Hotel",
                "Nagaraju",
                "nagaraju@gmail.com",
                "password123",
                "9876543210",
                "Hyderabad",
                "Telangana",
                "India",
                "500001"
        );

        loginRequest = new LoginRequest("nagaraju@gmail.com", "password123");

        sampleHotel = Hotel.builder()
                .id(1L)
                .name("Grand Palace Hotel")
                .status(HotelStatus.ACTIVE)
                .build();

        sampleUser = User.builder()
                .id(10L)
                .hotel(sampleHotel)
                .name("Nagaraju")
                .email("nagaraju@gmail.com")
                .password("encodedPassword")
                .role(UserRole.OWNER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    public void testSignupSuccess() {
        when(userRepository.existsByEmail("nagaraju@gmail.com")).thenReturn(false);
        when(hotelRepository.save(any(Hotel.class))).thenReturn(sampleHotel);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("mockJwtToken");

        AuthResponse response = authService.signup(signupRequest);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.message(), "Signup successful");
        Assert.assertEquals(response.token(), "mockJwtToken");
        Assert.assertEquals(response.user().name(), "Nagaraju");
        Assert.assertEquals(response.hotel().name(), "Grand Palace Hotel");

        verify(hotelRepository).save(any(Hotel.class));
        verify(userRepository).save(any(User.class));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSignupDuplicateEmailThrowsException() {
        when(userRepository.existsByEmail("nagaraju@gmail.com")).thenReturn(true);

        authService.signup(signupRequest);
    }

    @Test
    public void testLoginSuccess() {
        when(userRepository.findByEmail("nagaraju@gmail.com")).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateToken(sampleUser)).thenReturn("mockJwtToken");

        AuthResponse response = authService.login(loginRequest);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.message(), "Login successful");
        Assert.assertEquals(response.token(), "mockJwtToken");
        Assert.assertEquals(response.user().email(), "nagaraju@gmail.com");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}
