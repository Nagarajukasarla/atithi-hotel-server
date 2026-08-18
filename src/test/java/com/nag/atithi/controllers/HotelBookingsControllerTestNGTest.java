package com.nag.atithi.controllers;

import com.nag.atithi.models.Hotel;
import com.nag.atithi.models.User;
import com.nag.atithi.models.UserPrincipal;
import com.nag.atithi.requests.CreateBookingRequest;
import com.nag.atithi.requests.UpdateBookingRequest;
import com.nag.atithi.responses.ApiResponse;
import com.nag.atithi.responses.BookingResponse;
import com.nag.atithi.services.BookingService;
import com.nag.atithi.services.PaymentService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class HotelBookingsControllerTestNGTest {

    @Mock
    private BookingService bookingService;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private HotelBookingsController hotelBookingsController;

    private AutoCloseable mocks;
    private UserPrincipal userPrincipal;
    private BookingResponse bookingResponse;

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
        Mockito.reset(bookingService);

        Hotel hotel = Hotel.builder().id(1L).name("Grand Palace Hotel").build();
        User user = User.builder().id(10L).hotel(hotel).email("owner@hotel.com").build();
        userPrincipal = new UserPrincipal(user);

        bookingResponse = BookingResponse.builder()
                .id(501L)
                .hotelId(1L)
                .roomId(101L)
                .roomNumber("101")
                .totalPrice(2000.0)
                .status("CONFIRMED")
                .build();
    }

    @Test
    public void testCreateBookingSuccess() {
        CreateBookingRequest request = new CreateBookingRequest();
        when(bookingService.createBooking(eq(1L), any(CreateBookingRequest.class))).thenReturn(bookingResponse);

        ResponseEntity<ApiResponse<BookingResponse>> response = hotelBookingsController.createBooking(userPrincipal, request);

        Assert.assertEquals(response.getStatusCode(), HttpStatus.CREATED);
        Assert.assertNotNull(response.getBody());
        Assert.assertTrue(response.getBody().success());
        Assert.assertEquals(response.getBody().data().getId(), Long.valueOf(501L));
        verify(bookingService, times(1)).createBooking(eq(1L), any(CreateBookingRequest.class));
    }

    @Test
    public void testGetAllBookingsSuccess() {
        java.util.List<BookingResponse> list = java.util.List.of(bookingResponse);
        when(bookingService.getAllBookings(eq(1L))).thenReturn(list);

        ResponseEntity<ApiResponse<java.util.List<BookingResponse>>> response = hotelBookingsController.getAllBookings(userPrincipal);

        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(response.getBody());
        Assert.assertTrue(response.getBody().success());
        Assert.assertEquals(response.getBody().data().size(), 1);
        verify(bookingService, times(1)).getAllBookings(eq(1L));
    }

    @Test
    public void testCreateBookingUnauthenticated() {
        CreateBookingRequest request = new CreateBookingRequest();
        ResponseEntity<ApiResponse<BookingResponse>> response = hotelBookingsController.createBooking(null, request);

        Assert.assertEquals(response.getStatusCode(), HttpStatus.UNAUTHORIZED);
        Assert.assertNotNull(response.getBody());
        Assert.assertFalse(response.getBody().success());
    }

    @Test
    public void testUpdateBookingSuccess() {
        UpdateBookingRequest request = new UpdateBookingRequest();
        when(bookingService.updateBooking(eq(1L), eq(501L), any(UpdateBookingRequest.class))).thenReturn(bookingResponse);

        ResponseEntity<ApiResponse<BookingResponse>> response = hotelBookingsController.updateBooking(userPrincipal, 501L, request);

        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(response.getBody());
        Assert.assertTrue(response.getBody().success());
        Assert.assertEquals(response.getBody().data().getId(), Long.valueOf(501L));
        verify(bookingService, times(1)).updateBooking(eq(1L), eq(501L), any(UpdateBookingRequest.class));
    }

    @Test
    public void testCheckInBookingSuccess() {
        when(bookingService.checkInBooking(eq(1L), eq(501L))).thenReturn(bookingResponse);
        ResponseEntity<ApiResponse<BookingResponse>> response = hotelBookingsController.checkInBooking(userPrincipal, 501L);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(response.getBody());
        Assert.assertTrue(response.getBody().success());
        verify(bookingService, times(1)).checkInBooking(eq(1L), eq(501L));
    }

    @Test
    public void testCheckOutBookingSuccess() {
        when(bookingService.checkOutBooking(eq(1L), eq(501L))).thenReturn(bookingResponse);
        ResponseEntity<ApiResponse<BookingResponse>> response = hotelBookingsController.checkOutBooking(userPrincipal, 501L);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(response.getBody());
        Assert.assertTrue(response.getBody().success());
        verify(bookingService, times(1)).checkOutBooking(eq(1L), eq(501L));
    }

    @Test
    public void testGetBookingsCheckingOutSoonSuccess() {
        java.util.List<BookingResponse> list = java.util.List.of(bookingResponse);
        when(bookingService.getBookingsCheckingOutSoon(eq(1L))).thenReturn(list);
        ResponseEntity<ApiResponse<java.util.List<BookingResponse>>> response = hotelBookingsController.getBookingsCheckingOutSoon(userPrincipal);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(response.getBody());
        Assert.assertTrue(response.getBody().success());
        Assert.assertEquals(response.getBody().data().size(), 1);
        verify(bookingService, times(1)).getBookingsCheckingOutSoon(eq(1L));
    }
}
