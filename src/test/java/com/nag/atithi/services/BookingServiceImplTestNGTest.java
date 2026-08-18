package com.nag.atithi.services;

import com.nag.atithi.enums.BookingStatus;
import com.nag.atithi.enums.HotelStatus;
import com.nag.atithi.enums.RoomStatus;
import com.nag.atithi.implementations.BookingServiceImpl;
import com.nag.atithi.models.*;
import com.nag.atithi.repositories.*;
import com.nag.atithi.requests.CreateBookingRequest;
import com.nag.atithi.requests.GuestRequest;
import com.nag.atithi.responses.BookingResponse;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class BookingServiceImplTestNGTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private HotelRepository hotelRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

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
        Mockito.reset(bookingRepository, roomRepository, hotelRepository);

        sampleHotel = Hotel.builder()
                .id(1L)
                .name("Grand Palace Hotel")
                .status(HotelStatus.ACTIVE)
                .build();

        sampleRoomType = RoomType.builder()
                .id(10L)
                .hotel(sampleHotel)
                .name("Deluxe Room")
                .basePrice(BigDecimal.valueOf(1000.0))
                .build();

        sampleRoom = Room.builder()
                .id(101L)
                .hotel(sampleHotel)
                .roomNumber("101")
                .floor("1st Floor")
                .roomType(sampleRoomType)
                .capacity(2)
                .roomStatus(RoomStatus.VACANT)
                .build();
    }

    @Test
    public void testCreateBooking_Success_DailyRate() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(sampleHotel));
        when(roomRepository.findByIdAndHotelId(101L, 1L)).thenReturn(Optional.of(sampleRoom));
        
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(501L);
            return b;
        });

        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(101L);
        request.setCheckIn(LocalDateTime.of(2026, 8, 16, 12, 0));
        request.setCheckOut(LocalDateTime.of(2026, 8, 18, 12, 0)); // 2 days
        request.setIsHourly(false);

        GuestRequest gRequest = new GuestRequest();
        gRequest.setName("John Doe");
        gRequest.setAadhaarNumber("123456789012");
        gRequest.setMobileNumber("9876543210");
        gRequest.setAge(30);
        gRequest.setAddress("Some Address");
        request.setGuests(List.of(gRequest));

        BookingResponse response = bookingService.createBooking(1L, request);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getId(), Long.valueOf(501L));
        Assert.assertEquals(response.getTotalPrice(), 2000.0); // 2 days * 1000.0 = 2000.0
        Assert.assertEquals(response.getRoomNumber(), "101");
        Assert.assertEquals(response.getGuests().size(), 1);
        Assert.assertEquals(response.getGuests().get(0).getName(), "John Doe");
        Assert.assertEquals(sampleRoom.getRoomStatus(), RoomStatus.OCCUPIED);

        verify(hotelRepository, times(1)).findById(1L);
        verify(roomRepository, times(1)).findByIdAndHotelId(101L, 1L);
        verify(roomRepository, times(1)).save(sampleRoom);
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    public void testCreateBooking_Success_HourlyRate() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(sampleHotel));
        when(roomRepository.findByIdAndHotelId(101L, 1L)).thenReturn(Optional.of(sampleRoom));
        
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(502L);
            return b;
        });

        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(101L);
        request.setCheckIn(LocalDateTime.of(2026, 8, 16, 12, 0));
        request.setCheckOut(LocalDateTime.of(2026, 8, 16, 15, 0)); // 3 hours
        request.setIsHourly(true);

        GuestRequest gRequest = new GuestRequest();
        gRequest.setName("Jane Doe");
        gRequest.setAadhaarNumber("987654321098");
        gRequest.setMobileNumber("8765432109");
        gRequest.setAge(28);
        gRequest.setAddress("Other Address");
        request.setGuests(List.of(gRequest));

        BookingResponse response = bookingService.createBooking(1L, request);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getId(), Long.valueOf(502L));
        // hourly rate = Math.round(1000 / 10.0) = 100.
        // hours = 3. Total price = 3 * 100 = 300.0.
        Assert.assertEquals(response.getTotalPrice(), 300.0);
        Assert.assertEquals(response.getRoomNumber(), "101");
        Assert.assertEquals(sampleRoom.getRoomStatus(), RoomStatus.OCCUPIED);

        verify(hotelRepository, times(1)).findById(1L);
        verify(roomRepository, times(1)).findByIdAndHotelId(101L, 1L);
        verify(roomRepository, times(1)).save(sampleRoom);
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testCreateBooking_RoomNotVacant_ThrowsException() {
        sampleRoom.setRoomStatus(RoomStatus.OCCUPIED);
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(sampleHotel));
        when(roomRepository.findByIdAndHotelId(101L, 1L)).thenReturn(Optional.of(sampleRoom));

        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(101L);
        request.setCheckIn(LocalDateTime.of(2026, 8, 16, 12, 0));
        request.setCheckOut(LocalDateTime.of(2026, 8, 17, 12, 0));
        request.setIsHourly(false);
        request.setGuests(List.of(new GuestRequest()));

        bookingService.createBooking(1L, request);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreateBooking_InvalidDates_ThrowsException() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(sampleHotel));
        when(roomRepository.findByIdAndHotelId(101L, 1L)).thenReturn(Optional.of(sampleRoom));

        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(101L);
        request.setCheckIn(LocalDateTime.of(2026, 8, 16, 12, 0));
        request.setCheckOut(LocalDateTime.of(2026, 8, 16, 11, 0)); // checkout before checkin
        request.setIsHourly(false);
        request.setGuests(List.of(new GuestRequest()));

        bookingService.createBooking(1L, request);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreateBooking_HotelNotFound_ThrowsException() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.empty());

        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(101L);
        request.setCheckIn(LocalDateTime.of(2026, 8, 16, 12, 0));
        request.setCheckOut(LocalDateTime.of(2026, 8, 17, 12, 0));
        request.setIsHourly(false);

        bookingService.createBooking(1L, request);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreateBooking_RoomNotFound_ThrowsException() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(sampleHotel));
        when(roomRepository.findByIdAndHotelId(101L, 1L)).thenReturn(Optional.empty());

        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(101L);
        request.setCheckIn(LocalDateTime.of(2026, 8, 16, 12, 0));
        request.setCheckOut(LocalDateTime.of(2026, 8, 17, 12, 0));
        request.setIsHourly(false);

        bookingService.createBooking(1L, request);
    }

    @Test
    public void testCheckInBooking_Success() {
        Booking booking = Booking.builder()
                .id(501L)
                .hotel(sampleHotel)
                .room(sampleRoom)
                .status(BookingStatus.CONFIRMED)
                .build();
        when(bookingRepository.findById(501L)).thenReturn(Optional.of(booking));
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        BookingResponse response = bookingService.checkInBooking(1L, 501L);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatus(), "CHECKED_IN");
        Assert.assertEquals(sampleRoom.getRoomStatus(), RoomStatus.OCCUPIED);
        verify(bookingRepository, times(1)).findById(501L);
        verify(roomRepository, times(1)).save(sampleRoom);
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    public void testCheckOutBooking_Success() {
        Booking booking = Booking.builder()
                .id(501L)
                .hotel(sampleHotel)
                .room(sampleRoom)
                .status(BookingStatus.CHECKED_IN)
                .build();
        when(bookingRepository.findById(501L)).thenReturn(Optional.of(booking));
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        BookingResponse response = bookingService.checkOutBooking(1L, 501L);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatus(), "CHECKED_OUT");
        Assert.assertEquals(sampleRoom.getRoomStatus(), RoomStatus.VACANT);
        Assert.assertEquals(sampleRoom.getRoomState(), com.nag.atithi.enums.RoomState.DIRTY);
        verify(bookingRepository, times(1)).findById(501L);
        verify(roomRepository, times(1)).save(sampleRoom);
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    public void testGetBookingsCheckingOutSoon_Success() {
        Booking booking = Booking.builder()
                .id(501L)
                .hotel(sampleHotel)
                .room(sampleRoom)
                .checkIn(LocalDateTime.now())
                .checkOut(LocalDateTime.now().plusMinutes(30))
                .totalPrice(1500.0)
                .status(BookingStatus.CHECKED_IN)
                .build();
        when(bookingRepository.findByHotelIdAndStatusInAndCheckOutBetween(
                eq(1L), any(), any(), any()
        )).thenReturn(List.of(booking));

        List<BookingResponse> responses = bookingService.getBookingsCheckingOutSoon(1L);

        Assert.assertNotNull(responses);
        Assert.assertEquals(responses.size(), 1);
        Assert.assertEquals(responses.get(0).getId(), Long.valueOf(501L));
    }

    @Test
    public void testUpdateBooking_Success() {
        Booking booking = Booking.builder()
                .id(501L)
                .hotel(sampleHotel)
                .room(sampleRoom)
                .checkIn(LocalDateTime.of(2026, 8, 16, 12, 0))
                .checkOut(LocalDateTime.of(2026, 8, 18, 12, 0))
                .totalPrice(2000.0)
                .isHourly(false)
                .guests(new ArrayList<>())
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findById(501L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        com.nag.atithi.requests.UpdateBookingRequest request = new com.nag.atithi.requests.UpdateBookingRequest();
        request.setCheckIn(LocalDateTime.of(2026, 8, 17, 12, 0));
        request.setCheckOut(LocalDateTime.of(2026, 8, 19, 12, 0));
        request.setTotalPrice(2500.0);
        request.setIsHourly(false);

        GuestRequest gRequest = new GuestRequest();
        gRequest.setName("Jane Doe");
        gRequest.setAadhaarNumber("987654321098");
        gRequest.setMobileNumber("8765432109");
        gRequest.setAge(28);
        gRequest.setAddress("Other Address");
        request.setGuests(List.of(gRequest));

        BookingResponse response = bookingService.updateBooking(1L, 501L, request);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getTotalPrice(), 2500.0);
        Assert.assertEquals(booking.getCheckIn(), LocalDateTime.of(2026, 8, 17, 12, 0));
        Assert.assertEquals(booking.getCheckOut(), LocalDateTime.of(2026, 8, 19, 12, 0));
        Assert.assertEquals(booking.getGuests().size(), 1);
        Assert.assertEquals(booking.getGuests().get(0).getName(), "Jane Doe");

        verify(bookingRepository, times(1)).findById(501L);
        verify(bookingRepository, times(1)).save(booking);
    }
}
