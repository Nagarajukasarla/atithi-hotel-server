package com.nag.atithi.controllers;

import com.nag.atithi.models.BedType;
import com.nag.atithi.models.Hotel;
import com.nag.atithi.models.User;
import com.nag.atithi.models.UserPrincipal;
import com.nag.atithi.requests.UpdateBedsRequest;
import com.nag.atithi.responses.ApiResponse;
import com.nag.atithi.services.BedTypeService;
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

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class HotelBedsControllerTestNGTest {

    @Mock
    private BedTypeService bedTypeService;

    @InjectMocks
    private HotelBedsController hotelBedsController;

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
        Mockito.reset(bedTypeService);

        Hotel hotel = Hotel.builder().id(1L).name("Grand Palace Hotel").build();
        User user = User.builder().id(10L).hotel(hotel).email("owner@hotel.com").build();
        userPrincipal = new UserPrincipal(user);
    }

    @Test
    public void testGetBedsSuccess() {
        when(bedTypeService.getHotelBeds(1L)).thenReturn(List.of(
                BedType.builder().id(101L).name("Double Bed").build(),
                BedType.builder().id(102L).name("Single Bed").build()
        ));

        ResponseEntity<ApiResponse<List<BedType>>> response = hotelBedsController.getBeds(userPrincipal);

        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(response.getBody());
        Assert.assertTrue(response.getBody().success());
        Assert.assertEquals(response.getBody().data().size(), 2);
        Assert.assertEquals(response.getBody().data().get(0).getName(), "Double Bed");
    }

    @Test
    public void testGetBedsUnauthenticated() {
        ResponseEntity<ApiResponse<List<BedType>>> response = hotelBedsController.getBeds(null);

        Assert.assertEquals(response.getStatusCode(), HttpStatus.UNAUTHORIZED);
        Assert.assertNotNull(response.getBody());
        Assert.assertFalse(response.getBody().success());
    }

    @Test
    public void testSaveBedsSuccess() {
        UpdateBedsRequest request = new UpdateBedsRequest();
        request.setBeds(new ArrayList<>());

        ResponseEntity<ApiResponse<Void>> response = hotelBedsController.saveBeds(userPrincipal, request);

        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(response.getBody());
        Assert.assertTrue(response.getBody().success());
        verify(bedTypeService, times(1)).updateHotelBeds(1L, request.getBeds());
    }

    @Test
    public void testSaveBedsUnauthenticated() {
        UpdateBedsRequest request = new UpdateBedsRequest();
        request.setBeds(new ArrayList<>());

        ResponseEntity<ApiResponse<Void>> response = hotelBedsController.saveBeds(null, request);

        Assert.assertEquals(response.getStatusCode(), HttpStatus.UNAUTHORIZED);
        Assert.assertNotNull(response.getBody());
        Assert.assertFalse(response.getBody().success());
        verify(bedTypeService, never()).updateHotelBeds(anyLong(), anyList());
    }
}
