package com.nag.atithi.controllers;

import com.nag.atithi.models.Hotel;
import com.nag.atithi.models.User;
import com.nag.atithi.models.UserPrincipal;
import com.nag.atithi.requests.RoomTypeRequest;
import com.nag.atithi.responses.ApiResponse;
import com.nag.atithi.responses.RoomTypeResponse;
import com.nag.atithi.services.RoomTypeService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;

public class RoomTypesControllerTestNGTest {

    @Mock
    private RoomTypeService roomTypeService;

    @InjectMocks
    private RoomTypesController roomTypesController;

    private AutoCloseable mocks;
    private UserPrincipal userPrincipal;
    private RoomTypeResponse roomTypeResponse;

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
        Mockito.reset(roomTypeService);

        Hotel hotel = Hotel.builder().id(1L).name("Grand Palace Hotel").build();
        User user = User.builder().id(10L).hotel(hotel).email("owner@hotel.com").build();
        userPrincipal = new UserPrincipal(user);

        roomTypeResponse = new RoomTypeResponse(
                101L,
                "Deluxe Room",
                "City view luxury",
                BigDecimal.valueOf(3500.00),
                2,
                1,
                3,
                "Double Bed",
                "350 sq.ft",
                "ACTIVE",
                List.of("wifi"),
                true,
                "FEMALE"
        );
    }

    @Test
    public void testGetRoomTypesSuccess() {
        when(roomTypeService.getHotelRoomTypes(1L)).thenReturn(List.of(roomTypeResponse));

        ResponseEntity<ApiResponse<List<RoomTypeResponse>>> response = roomTypesController.getRoomTypes(userPrincipal);

        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(response.getBody());
        Assert.assertTrue(response.getBody().success());
        Assert.assertEquals(response.getBody().data().size(), 1);
        Assert.assertEquals(response.getBody().data().get(0).name(), "Deluxe Room");
    }

    @Test
    public void testGetRoomTypesUnauthenticated() {
        ResponseEntity<ApiResponse<List<RoomTypeResponse>>> response = roomTypesController.getRoomTypes(null);

        Assert.assertEquals(response.getStatusCode(), HttpStatus.UNAUTHORIZED);
        Assert.assertNotNull(response.getBody());
        Assert.assertFalse(response.getBody().success());
    }

    @Test
    public void testCreateRoomTypeSuccess() {
        RoomTypeRequest request = new RoomTypeRequest();
        when(roomTypeService.createRoomType(eq(1L), any(RoomTypeRequest.class))).thenReturn(roomTypeResponse);

        ResponseEntity<ApiResponse<RoomTypeResponse>> response = roomTypesController.createRoomType(userPrincipal, request);

        Assert.assertEquals(response.getStatusCode(), HttpStatus.CREATED);
        Assert.assertNotNull(response.getBody());
        Assert.assertTrue(response.getBody().success());
        Assert.assertEquals(response.getBody().data().id(), Long.valueOf(101L));
        verify(roomTypeService, times(1)).createRoomType(eq(1L), any(RoomTypeRequest.class));
    }

    @Test
    public void testUpdateRoomTypeSuccess() {
        RoomTypeRequest request = new RoomTypeRequest();
        when(roomTypeService.updateRoomType(eq(1L), eq(101L), any(RoomTypeRequest.class))).thenReturn(roomTypeResponse);

        ResponseEntity<ApiResponse<RoomTypeResponse>> response = roomTypesController.updateRoomType(userPrincipal, 101L, request);

        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(response.getBody());
        Assert.assertTrue(response.getBody().success());
        Assert.assertEquals(response.getBody().data().name(), "Deluxe Room");
        verify(roomTypeService, times(1)).updateRoomType(eq(1L), eq(101L), any(RoomTypeRequest.class));
    }

    @Test
    public void testDeleteRoomTypeSuccess() {
        ResponseEntity<ApiResponse<Void>> response = roomTypesController.deleteRoomType(userPrincipal, 101L);

        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(response.getBody());
        Assert.assertTrue(response.getBody().success());
        verify(roomTypeService, times(1)).deleteRoomType(1L, 101L);
    }
}
