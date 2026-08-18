package com.nag.atithi.controllers;

import com.nag.atithi.models.User;
import com.nag.atithi.models.Hotel;
import com.nag.atithi.models.UserPrincipal;
import com.nag.atithi.requests.UpdateAmenitiesRequest;
import com.nag.atithi.responses.ApiResponse;
import com.nag.atithi.services.AmenityService;
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

import java.util.List;

import static org.mockito.Mockito.*;

public class HotelAmenitiesControllerTestNGTest {

    @Mock
    private AmenityService amenityService;

    @InjectMocks
    private HotelAmenitiesController hotelAmenitiesController;

    private AutoCloseable mocks;
    private UserPrincipal userPrincipal;

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
        Mockito.reset(amenityService);

        Hotel hotel = Hotel.builder().id(1L).name("Grand Palace Hotel").build();
        User user = User.builder().id(10L).hotel(hotel).email("test@hotel.com").build();
        userPrincipal = new UserPrincipal(user);
    }

    @Test
    public void testGetAmenitiesSuccess() {
        when(amenityService.getHotelAmenities(1L)).thenReturn(List.of("wifi", "ac"));

        ResponseEntity<ApiResponse<List<String>>> response = hotelAmenitiesController.getAmenities(userPrincipal);

        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(response.getBody());
        Assert.assertTrue(response.getBody().success());
        Assert.assertEquals(response.getBody().data().size(), 2);
        Assert.assertTrue(response.getBody().data().contains("wifi"));
    }

    @Test
    public void testGetAmenitiesUnauthenticated() {
        ResponseEntity<ApiResponse<List<String>>> response = hotelAmenitiesController.getAmenities(null);

        Assert.assertEquals(response.getStatusCode(), HttpStatus.UNAUTHORIZED);
        Assert.assertNotNull(response.getBody());
        Assert.assertFalse(response.getBody().success());
    }

    @Test
    public void testSaveAmenitiesSuccess() {
        UpdateAmenitiesRequest request = new UpdateAmenitiesRequest();
        request.setAmenities(List.of("wifi", "ac"));

        ResponseEntity<ApiResponse<Void>> response = hotelAmenitiesController.saveAmenities(userPrincipal, request);

        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(response.getBody());
        Assert.assertTrue(response.getBody().success());
        verify(amenityService, times(1)).updateHotelAmenities(1L, List.of("wifi", "ac"));
    }

    @Test
    public void testSaveAmenitiesUnauthenticated() {
        UpdateAmenitiesRequest request = new UpdateAmenitiesRequest();
        request.setAmenities(List.of("wifi", "ac"));

        ResponseEntity<ApiResponse<Void>> response = hotelAmenitiesController.saveAmenities(null, request);

        Assert.assertEquals(response.getStatusCode(), HttpStatus.UNAUTHORIZED);
        Assert.assertNotNull(response.getBody());
        Assert.assertFalse(response.getBody().success());
        verify(amenityService, never()).updateHotelAmenities(anyLong(), anyList());
    }
}
