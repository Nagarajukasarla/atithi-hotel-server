package com.nag.atithi.controllers;

import com.nag.atithi.enums.RoomState;
import com.nag.atithi.enums.RoomStatus;
import com.nag.atithi.models.Hotel;
import com.nag.atithi.models.Room;
import com.nag.atithi.models.RoomType;
import com.nag.atithi.models.User;
import com.nag.atithi.models.UserPrincipal;
import com.nag.atithi.requests.CheckAvailabilityRequest;
import com.nag.atithi.requests.CreateRoomRequest;
import com.nag.atithi.requests.UpdateRoomRequest;
import com.nag.atithi.responses.ApiResponse;
import com.nag.atithi.responses.RoomResponse;
import com.nag.atithi.services.RoomService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class HotelRoomsControllerTestNGTest {

    @Mock
    private RoomService roomService;

    @InjectMocks
    private HotelRoomsController hotelRoomsController;

    private AutoCloseable mocks;
    private UserPrincipal userPrincipal;
    private Room sampleRoom;

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
        Mockito.reset(roomService);

        Hotel hotel = Hotel.builder().id(1L).name("Grand Palace Hotel").build();
        User user = User.builder().id(10L).hotel(hotel).email("owner@hotel.com").build();
        userPrincipal = new UserPrincipal(user);

        RoomType roomType = RoomType.builder().id(20L).name("Deluxe Room").build();
        sampleRoom = Room.builder()
                .id(101L)
                .hotel(hotel)
                .roomNumber("101")
                .floor("1st Floor")
                .roomType(roomType)
                .capacity(2)
                .roomStatus(RoomStatus.VACANT)
                .roomState(RoomState.CLEAN)
                .lastCleaned(LocalDateTime.now())
                .notes("Standard notes")
                .build();
    }

    @Test
    public void testGetRoomsSuccess() {
        when(roomService.getHotelRooms(1L)).thenReturn(List.of(sampleRoom));

        ResponseEntity<ApiResponse<List<RoomResponse>>> response = hotelRoomsController.getRooms(userPrincipal);

        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(response.getBody());
        Assert.assertTrue(response.getBody().success());
        Assert.assertEquals(response.getBody().data().size(), 1);
        Assert.assertEquals(response.getBody().data().get(0).getNumber(), "101");
    }

    @Test
    public void testGetRoomsUnauthenticated() {
        ResponseEntity<ApiResponse<List<RoomResponse>>> response = hotelRoomsController.getRooms(null);

        Assert.assertEquals(response.getStatusCode(), HttpStatus.UNAUTHORIZED);
        Assert.assertNotNull(response.getBody());
        Assert.assertFalse(response.getBody().success());
    }

    @Test
    public void testCreateRoomSuccess() {
        CreateRoomRequest request = new CreateRoomRequest();
        when(roomService.createRoom(eq(1L), any(CreateRoomRequest.class))).thenReturn(sampleRoom);

        ResponseEntity<ApiResponse<RoomResponse>> response = hotelRoomsController.createRoom(userPrincipal, request);

        Assert.assertEquals(response.getStatusCode(), HttpStatus.CREATED);
        Assert.assertNotNull(response.getBody());
        Assert.assertTrue(response.getBody().success());
        Assert.assertEquals(response.getBody().data().getId(), "101");
        verify(roomService, times(1)).createRoom(eq(1L), any(CreateRoomRequest.class));
    }

    @Test
    public void testUpdateRoomSuccess() {
        UpdateRoomRequest request = new UpdateRoomRequest();
        when(roomService.updateRoom(eq(1L), eq(101L), any(UpdateRoomRequest.class))).thenReturn(sampleRoom);

        ResponseEntity<ApiResponse<RoomResponse>> response = hotelRoomsController.updateRoom(userPrincipal, 101L, request);

        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(response.getBody());
        Assert.assertTrue(response.getBody().success());
        Assert.assertEquals(response.getBody().data().getNumber(), "101");
        verify(roomService, times(1)).updateRoom(eq(1L), eq(101L), any(UpdateRoomRequest.class));
    }

    @Test
    public void testDeleteRoomSuccess() {
        ResponseEntity<ApiResponse<Void>> response = hotelRoomsController.deleteRoom(userPrincipal, 101L);

        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(response.getBody());
        Assert.assertTrue(response.getBody().success());
        verify(roomService, times(1)).deleteRoom(1L, 101L);
    }

    @Test
    public void testCheckAvailabilitySuccess() {
        CheckAvailabilityRequest request = new CheckAvailabilityRequest();
        when(roomService.checkAvailability(eq(1L), any(CheckAvailabilityRequest.class))).thenReturn(List.of(sampleRoom));

        ResponseEntity<ApiResponse<List<RoomResponse>>> response = hotelRoomsController.checkAvailability(userPrincipal, request);

        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(response.getBody());
        Assert.assertTrue(response.getBody().success());
        Assert.assertEquals(response.getBody().data().size(), 1);
        Assert.assertEquals(response.getBody().data().get(0).getNumber(), "101");
        verify(roomService, times(1)).checkAvailability(eq(1L), any(CheckAvailabilityRequest.class));
    }

    @Test
    public void testCheckAvailabilityUnauthenticated() {
        CheckAvailabilityRequest request = new CheckAvailabilityRequest();
        ResponseEntity<ApiResponse<List<RoomResponse>>> response = hotelRoomsController.checkAvailability(null, request);

        Assert.assertEquals(response.getStatusCode(), HttpStatus.UNAUTHORIZED);
        Assert.assertNotNull(response.getBody());
        Assert.assertFalse(response.getBody().success());
    }
}
