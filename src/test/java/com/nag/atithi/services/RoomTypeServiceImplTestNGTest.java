package com.nag.atithi.services;

import com.nag.atithi.enums.HotelStatus;
import com.nag.atithi.enums.PremiumType;
import com.nag.atithi.enums.RoomTypeStatus;
import com.nag.atithi.implementations.RoomTypeServiceImpl;
import com.nag.atithi.models.Amenity;
import com.nag.atithi.models.Hotel;
import com.nag.atithi.models.RoomType;
import com.nag.atithi.repositories.AmenityRepository;
import com.nag.atithi.repositories.HotelRepository;
import com.nag.atithi.repositories.RoomTypeRepository;
import com.nag.atithi.requests.RoomTypeRequest;
import com.nag.atithi.responses.RoomTypeResponse;
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
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

public class RoomTypeServiceImplTestNGTest {

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private AmenityRepository amenityRepository;

    @InjectMocks
    private RoomTypeServiceImpl roomTypeService;

    private AutoCloseable mocks;
    private Hotel sampleHotel;
    private RoomType sampleRoomType;
    private Amenity amenity;

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
        Mockito.reset(hotelRepository, roomTypeRepository, amenityRepository);

        sampleHotel = Hotel.builder()
                .id(1L)
                .name("Grand Palace Hotel")
                .status(HotelStatus.ACTIVE)
                .build();

        amenity = Amenity.builder()
                .id(10L)
                .code("wifi")
                .name("Wi-Fi")
                .build();

        sampleRoomType = RoomType.builder()
                .id(101L)
                .hotel(sampleHotel)
                .name("Deluxe Room")
                .description("City view luxury")
                .basePrice(BigDecimal.valueOf(3500.00))
                .maxAdults(2)
                .maxChildren(1)
                .maxOccupancy(3)
                .bedType("Double Bed")
                .size("350 sq.ft")
                .status(RoomTypeStatus.ACTIVE)
                .isPremium(true)
                .premiumType(PremiumType.FEMALE)
                .amenities(new HashSet<>(List.of(amenity)))
                .build();
    }

    @Test
    public void testGetHotelRoomTypes() {
        when(roomTypeRepository.findAllByHotelId(1L)).thenReturn(List.of(sampleRoomType));

        List<RoomTypeResponse> responses = roomTypeService.getHotelRoomTypes(1L);

        Assert.assertNotNull(responses);
        Assert.assertEquals(responses.size(), 1);
        Assert.assertEquals(responses.get(0).name(), "Deluxe Room");
        Assert.assertTrue(responses.get(0).amenityIds().contains("wifi"));
        verify(roomTypeRepository, times(1)).findAllByHotelId(1L);
    }

    @Test
    public void testCreateRoomType() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(sampleHotel));
        when(amenityRepository.findAllByCodeIn(anyList())).thenReturn(List.of(amenity));
        when(roomTypeRepository.save(any(RoomType.class))).thenReturn(sampleRoomType);

        RoomTypeRequest request = new RoomTypeRequest();
        request.setName("Deluxe Room");
        request.setBasePrice(BigDecimal.valueOf(3500.00));
        request.setMaxAdults(2);
        request.setMaxChildren(1);
        request.setMaxOccupancy(3);
        request.setBedType("Double Bed");
        request.setSize("350 sq.ft");
        request.setStatus("ACTIVE");
        request.setAmenityIds(List.of("wifi"));
        request.setIsPremium(true);
        request.setPremiumType(PremiumType.FEMALE);

        RoomTypeResponse response = roomTypeService.createRoomType(1L, request);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.name(), "Deluxe Room");
        Assert.assertEquals(response.basePrice(), BigDecimal.valueOf(3500.00));
        Assert.assertTrue(response.amenityIds().contains("wifi"));
        Assert.assertTrue(response.isPremium());
        Assert.assertEquals(response.premiumType(), "FEMALE");
        verify(hotelRepository, times(1)).findById(1L);
        verify(amenityRepository, times(1)).findAllByCodeIn(anyList());
        verify(roomTypeRepository, times(1)).save(any(RoomType.class));
    }

    @Test
    public void testUpdateRoomType() {
        when(roomTypeRepository.findByHotelIdAndId(1L, 101L)).thenReturn(Optional.of(sampleRoomType));
        when(amenityRepository.findAllByCodeIn(anyList())).thenReturn(List.of(amenity));
        when(roomTypeRepository.save(any(RoomType.class))).thenReturn(sampleRoomType);

        RoomTypeRequest request = new RoomTypeRequest();
        request.setName("Updated Deluxe Room");
        request.setBasePrice(BigDecimal.valueOf(4000.00));
        request.setMaxAdults(2);
        request.setMaxChildren(2);
        request.setMaxOccupancy(4);
        request.setBedType("King Bed");
        request.setSize("380 sq.ft");
        request.setStatus("ACTIVE");
        request.setAmenityIds(List.of("wifi"));

        RoomTypeResponse response = roomTypeService.updateRoomType(1L, 101L, request);

        Assert.assertNotNull(response);
        Assert.assertEquals(sampleRoomType.getName(), "Updated Deluxe Room");
        Assert.assertEquals(sampleRoomType.getBasePrice(), BigDecimal.valueOf(4000.00));
        verify(roomTypeRepository, times(1)).findByHotelIdAndId(1L, 101L);
        verify(roomTypeRepository, times(1)).save(sampleRoomType);
    }

    @Test
    public void testDeleteRoomType() {
        when(roomTypeRepository.findByHotelIdAndId(1L, 101L)).thenReturn(Optional.of(sampleRoomType));

        roomTypeService.deleteRoomType(1L, 101L);

        verify(roomTypeRepository, times(1)).findByHotelIdAndId(1L, 101L);
        verify(roomTypeRepository, times(1)).delete(sampleRoomType);
    }

    @Test
    public void testUpdateRoomTypeTogglePremiumOff() {
        when(roomTypeRepository.findByHotelIdAndId(1L, 101L)).thenReturn(Optional.of(sampleRoomType));
        when(roomTypeRepository.save(any(RoomType.class))).thenReturn(sampleRoomType);

        RoomTypeRequest request = new RoomTypeRequest();
        request.setName("Deluxe Room");
        request.setBasePrice(BigDecimal.valueOf(3500.00));
        request.setMaxAdults(2);
        request.setMaxChildren(1);
        request.setMaxOccupancy(3);
        request.setStatus("ACTIVE");
        request.setIsPremium(false);
        request.setPremiumType(PremiumType.FEMALE);

        RoomTypeResponse response = roomTypeService.updateRoomType(1L, 101L, request);

        Assert.assertFalse(response.isPremium());
        Assert.assertNull(response.premiumType());
    }
}
