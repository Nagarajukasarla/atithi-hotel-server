package com.nag.atithi.services;

import com.nag.atithi.enums.AmenityStatus;
import com.nag.atithi.enums.HotelStatus;
import com.nag.atithi.implementations.AmenityServiceImpl;
import com.nag.atithi.models.Amenity;
import com.nag.atithi.models.Hotel;
import com.nag.atithi.repositories.AmenityRepository;
import com.nag.atithi.repositories.HotelRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

public class AmenityServiceImplTestNGTest {

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private AmenityRepository amenityRepository;

    @InjectMocks
    private AmenityServiceImpl amenityService;

    private AutoCloseable mocks;
    private Hotel sampleHotel;
    private Amenity amenity1;
    private Amenity amenity2;

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
        Mockito.reset(hotelRepository, amenityRepository);

        amenity1 = Amenity.builder()
                .id(1L)
                .code("wifi")
                .name("Wi-Fi")
                .icon("Wifi")
                .description("High-speed wireless internet access")
                .status(AmenityStatus.ACTIVE)
                .build();

        amenity2 = Amenity.builder()
                .id(2L)
                .code("ac")
                .name("Air Conditioning")
                .icon("Wind")
                .description("Climate-controlled cooling system")
                .status(AmenityStatus.ACTIVE)
                .build();

        Set<Amenity> amenitiesSet = new HashSet<>();
        amenitiesSet.add(amenity1);

        sampleHotel = Hotel.builder()
                .id(1L)
                .name("Grand Palace Hotel")
                .status(HotelStatus.ACTIVE)
                .amenities(amenitiesSet)
                .build();
    }

    @Test
    public void testGetHotelAmenities() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(sampleHotel));

        List<String> amenities = amenityService.getHotelAmenities(1L);

        Assert.assertNotNull(amenities);
        Assert.assertEquals(amenities.size(), 1);
        Assert.assertTrue(amenities.contains("wifi"));
        verify(hotelRepository, times(1)).findById(1L);
    }

    @Test
    public void testUpdateHotelAmenities() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(sampleHotel));
        when(amenityRepository.findAllByCodeIn(anyList())).thenReturn(List.of(amenity1, amenity2));
        when(hotelRepository.save(any(Hotel.class))).thenReturn(sampleHotel);

        amenityService.updateHotelAmenities(1L, List.of("wifi", "ac"));

        Assert.assertEquals(sampleHotel.getAmenities().size(), 2);
        Assert.assertTrue(sampleHotel.getAmenities().contains(amenity1));
        Assert.assertTrue(sampleHotel.getAmenities().contains(amenity2));
        verify(hotelRepository, times(1)).findById(1L);
        verify(amenityRepository, times(1)).findAllByCodeIn(anyList());
        verify(hotelRepository, times(1)).save(sampleHotel);
    }
}
