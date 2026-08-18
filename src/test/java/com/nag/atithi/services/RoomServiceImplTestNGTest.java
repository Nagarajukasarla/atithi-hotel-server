package com.nag.atithi.services;

import com.nag.atithi.enums.HotelStatus;
import com.nag.atithi.enums.RoomState;
import com.nag.atithi.enums.RoomStatus;
import com.nag.atithi.implementations.RoomServiceImpl;
import com.nag.atithi.models.Hotel;
import com.nag.atithi.models.Room;
import com.nag.atithi.models.RoomType;
import com.nag.atithi.repositories.HotelRepository;
import com.nag.atithi.repositories.RoomRepository;
import com.nag.atithi.repositories.RoomTypeRepository;
import com.nag.atithi.requests.CheckAvailabilityRequest;
import com.nag.atithi.requests.CreateRoomRequest;
import com.nag.atithi.requests.UpdateRoomRequest;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class RoomServiceImplTestNGTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @InjectMocks
    private RoomServiceImpl roomService;

    private AutoCloseable mocks;
    private Hotel sampleHotel;
    private RoomType sampleRoomType;
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
        Mockito.reset(roomRepository, hotelRepository, roomTypeRepository);

        sampleHotel = Hotel.builder()
                .id(1L)
                .name("Grand Palace Hotel")
                .status(HotelStatus.ACTIVE)
                .build();

        sampleRoomType = RoomType.builder()
                .id(10L)
                .hotel(sampleHotel)
                .name("Deluxe Room")
                .build();

        sampleRoom = Room.builder()
                .id(101L)
                .hotel(sampleHotel)
                .roomNumber("101")
                .floor("1st Floor")
                .roomType(sampleRoomType)
                .capacity(2)
                .roomStatus(RoomStatus.VACANT)
                .roomState(RoomState.CLEAN)
                .lastCleaned(LocalDateTime.now())
                .notes("Standard notes")
                .build();
    }

    @Test
    public void testGetHotelRooms() {
        when(roomRepository.findAllByHotelIdOrderByRoomNumberAsc(1L)).thenReturn(List.of(sampleRoom));

        List<Room> rooms = roomService.getHotelRooms(1L);

        Assert.assertNotNull(rooms);
        Assert.assertEquals(rooms.size(), 1);
        Assert.assertEquals(rooms.get(0).getRoomNumber(), "101");
        verify(roomRepository, times(1)).findAllByHotelIdOrderByRoomNumberAsc(1L);
    }

    @Test
    public void testCreateRoomSuccess() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(sampleHotel));
        when(roomRepository.existsByHotelIdAndRoomNumber(1L, "101")).thenReturn(false);
        when(roomTypeRepository.findByHotelIdAndName(1L, "Deluxe Room")).thenReturn(Optional.of(sampleRoomType));
        when(roomRepository.save(any(Room.class))).thenReturn(sampleRoom);

        CreateRoomRequest request = new CreateRoomRequest();
        request.setNumber("101");
        request.setFloor("1st Floor");
        request.setRoomType("Deluxe Room");
        request.setCapacity(2);
        request.setNotes("Standard notes");

        Room created = roomService.createRoom(1L, request);

        Assert.assertNotNull(created);
        Assert.assertEquals(created.getRoomNumber(), "101");
        Assert.assertEquals(created.getRoomState(), RoomState.CLEAN);
        Assert.assertEquals(created.getRoomStatus(), RoomStatus.VACANT);
        verify(hotelRepository, times(1)).findById(1L);
        verify(roomRepository, times(1)).existsByHotelIdAndRoomNumber(1L, "101");
        verify(roomTypeRepository, times(1)).findByHotelIdAndName(1L, "Deluxe Room");
        verify(roomRepository, times(1)).save(any(Room.class));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreateRoomDuplicateRoomNumberThrowsException() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(sampleHotel));
        when(roomRepository.existsByHotelIdAndRoomNumber(1L, "101")).thenReturn(true);

        CreateRoomRequest request = new CreateRoomRequest();
        request.setNumber("101");
        request.setFloor("1st Floor");
        request.setRoomType("Deluxe Room");
        request.setCapacity(2);

        roomService.createRoom(1L, request);
    }

    @Test
    public void testUpdateRoomSuccess() {
        when(roomRepository.findByIdAndHotelId(101L, 1L)).thenReturn(Optional.of(sampleRoom));
        when(roomRepository.save(any(Room.class))).thenReturn(sampleRoom);

        UpdateRoomRequest request = new UpdateRoomRequest();
        request.setNumber("101-A");
        request.setFloor("2nd Floor");
        request.setNotes("Updated Notes");
        request.setRoomState(RoomState.CLEAN);

        Room updated = roomService.updateRoom(1L, 101L, request);

        Assert.assertNotNull(updated);
        Assert.assertEquals(sampleRoom.getRoomNumber(), "101-A");
        Assert.assertEquals(sampleRoom.getFloor(), "2nd Floor");
        Assert.assertEquals(sampleRoom.getNotes(), "Updated Notes");
        verify(roomRepository, times(1)).findByIdAndHotelId(101L, 1L);
        verify(roomRepository, times(1)).save(sampleRoom);
    }

    @Test
    public void testDeleteRoomSuccess() {
        when(roomRepository.findByIdAndHotelId(101L, 1L)).thenReturn(Optional.of(sampleRoom));

        roomService.deleteRoom(1L, 101L);

        verify(roomRepository, times(1)).findByIdAndHotelId(101L, 1L);
        verify(roomRepository, times(1)).delete(sampleRoom);
    }

    @Test
    public void testCheckAvailability() {
        when(roomRepository.findAvailableRooms(eq(1L), eq("Deluxe Room"), eq(2), eq(RoomStatus.VACANT)))
                .thenReturn(List.of(sampleRoom));

        CheckAvailabilityRequest request = new CheckAvailabilityRequest();
        request.setRoomType("Deluxe Room");
        request.setMaxOccupancy(2);
        request.setStatus("Vacant");

        List<Room> result = roomService.checkAvailability(1L, request);

        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 1);
        Assert.assertEquals(result.get(0).getRoomNumber(), "101");
        verify(roomRepository, times(1))
                .findAvailableRooms(eq(1L), eq("Deluxe Room"), eq(2), eq(RoomStatus.VACANT));
    }
}
