package com.nag.atithi.security;

import com.nag.atithi.enums.HotelStatus;
import com.nag.atithi.enums.UserRole;
import com.nag.atithi.enums.UserStatus;
import com.nag.atithi.models.Hotel;
import com.nag.atithi.models.User;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class JwtServiceTestNGTest {

    private JwtService jwtService;

    @BeforeMethod
    public void setUp() {
        KeyLoader keyLoader = new KeyLoader();
        keyLoader.initKeys();

        jwtService = new JwtService(keyLoader);
        ReflectionTestUtils.setField(jwtService, "expirationSeconds", 86400L);
    }

    @Test
    public void testGenerateAndValidateToken() {
        Hotel hotel = Hotel.builder()
                .id(100L)
                .name("Grand Palace Hotel")
                .status(HotelStatus.ACTIVE)
                .build();

        User user = User.builder()
                .id(1L)
                .name("Nagaraju")
                .email("nagaraju@gmail.com")
                .role(UserRole.OWNER)
                .status(UserStatus.ACTIVE)
                .hotel(hotel)
                .build();

        String token = jwtService.generateToken(user);
        Assert.assertNotNull(token, "Token should not be null");
        Assert.assertTrue(jwtService.isTokenValid(token), "Token should be valid");

        Assert.assertEquals(jwtService.extractEmail(token), "nagaraju@gmail.com");
        Assert.assertEquals(jwtService.extractUserId(token), Long.valueOf(1L));
        Assert.assertEquals(jwtService.extractHotelId(token), Long.valueOf(100L));
        Assert.assertEquals(jwtService.extractRole(token), "OWNER");
    }
}
