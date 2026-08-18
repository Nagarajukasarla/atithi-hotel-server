package com.nag.atithi.controllers;

import com.nag.atithi.enums.HotelStatus;
import com.nag.atithi.enums.UserRole;
import com.nag.atithi.enums.UserStatus;
import com.nag.atithi.models.Hotel;
import com.nag.atithi.models.User;
import com.nag.atithi.models.UserPrincipal;
import com.nag.atithi.requests.LoginRequest;
import com.nag.atithi.requests.SignupRequest;
import com.nag.atithi.responses.ApiResponse;
import com.nag.atithi.responses.AuthResponse;
import com.nag.atithi.responses.HotelDto;
import com.nag.atithi.responses.UserDto;
import com.nag.atithi.security.CookieService;
import com.nag.atithi.services.AuthService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class AuthControllerTestNGTest {

    @Mock
    private AuthService authService;

    @Mock
    private CookieService cookieService;

    @InjectMocks
    private AuthController authController;

    private AutoCloseable mocks;
    private SignupRequest signupRequest;
    private LoginRequest loginRequest;
    private AuthResponse authResponse;
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
        Mockito.reset(authService, cookieService);

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

        Hotel hotel = Hotel.builder().id(1L).name("Grand Palace Hotel").status(HotelStatus.ACTIVE).build();
        sampleUser = User.builder().id(10L).hotel(hotel).name("Nagaraju").email("nagaraju@gmail.com").role(UserRole.OWNER).status(UserStatus.ACTIVE).build();

        authResponse = new AuthResponse(
                "Success",
                "token123",
                UserDto.fromEntity(sampleUser),
                HotelDto.fromEntity(hotel)
        );
    }

    @Test
    public void testSignupEndpoint() {
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        ResponseCookie cookie = ResponseCookie.from("jwt", "token123").build();

        when(authService.signup(any())).thenReturn(authResponse);
        when(cookieService.createJwtCookie("token123")).thenReturn(cookie);

        ResponseEntity<AuthResponse> responseEntity = authController.signup(signupRequest, servletResponse);

        Assert.assertEquals(responseEntity.getStatusCode(), HttpStatus.CREATED);
        Assert.assertNotNull(responseEntity.getBody());
        Assert.assertEquals(responseEntity.getBody().token(), "token123");
        Assert.assertTrue(servletResponse.getHeaderNames().contains("Set-Cookie"));
    }

    @Test
    public void testLoginEndpoint() {
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        ResponseCookie cookie = ResponseCookie.from("jwt", "token123").build();

        when(authService.login(any())).thenReturn(authResponse);
        when(cookieService.createJwtCookie("token123")).thenReturn(cookie);

        ResponseEntity<AuthResponse> responseEntity = authController.login(loginRequest, servletResponse);

        Assert.assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(responseEntity.getBody());
        Assert.assertEquals(responseEntity.getBody().token(), "token123");
    }

    @Test
    public void testLogoutEndpoint() {
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        ResponseCookie cookie = ResponseCookie.from("jwt", "").maxAge(0).build();

        when(cookieService.clearJwtCookie()).thenReturn(cookie);

        ResponseEntity<ApiResponse<Void>> responseEntity = authController.logout(servletResponse);

        Assert.assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
        Assert.assertTrue(responseEntity.getBody().success());
    }

    @Test
    public void testGetCurrentUserEndpoint() {
        UserPrincipal principal = new UserPrincipal(sampleUser);
        ResponseEntity<ApiResponse<UserDto>> responseEntity = authController.getCurrentUser(principal);

        Assert.assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(responseEntity.getBody());
        Assert.assertEquals(responseEntity.getBody().data().email(), "nagaraju@gmail.com");
    }
}
