package com.nag.atithi.services;

import com.nag.atithi.enums.BedStatus;
import com.nag.atithi.enums.HotelStatus;
import com.nag.atithi.implementations.BedTypeServiceImpl;
import com.nag.atithi.models.BedType;
import com.nag.atithi.models.Hotel;
import com.nag.atithi.repositories.BedTypeRepository;
import com.nag.atithi.repositories.HotelRepository;
import com.nag.atithi.requests.BedTypeRequest;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

public class BedTypeServiceImplTestNGTest {

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private BedTypeRepository bedTypeRepository;

    @InjectMocks
    private BedTypeServiceImpl bedTypeService;

    private AutoCloseable mocks;
    private Hotel sampleHotel;
    private BedType bedType1;
    private BedType bedType2;

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
        Mockito.reset(hotelRepository, bedTypeRepository);

        sampleHotel = Hotel.builder()
                .id(1L)
                .name("Grand Palace Hotel")
                .status(HotelStatus.ACTIVE)
                .build();

        bedType1 = BedType.builder()
                .id(101L)
                .hotel(sampleHotel)
                .name("Double Bed")
                .description("Standard double bed configuration")
                .capacity(3)
                .maxAdults(2)
                .maxChildren(1)
                .status(BedStatus.ACTIVE)
                .build();

        bedType2 = BedType.builder()
                .id(102L)
                .hotel(sampleHotel)
                .name("Single Bed")
                .description("Standard single bed configuration")
                .capacity(1)
                .maxAdults(1)
                .maxChildren(0)
                .status(BedStatus.ACTIVE)
                .build();
    }

    @Test
    public void testGetHotelBeds() {
        when(bedTypeRepository.findAllByHotelId(1L)).thenReturn(List.of(bedType1, bedType2));

        List<BedType> beds = bedTypeService.getHotelBeds(1L);

        Assert.assertNotNull(beds);
        Assert.assertEquals(beds.size(), 2);
        Assert.assertEquals(beds.get(0).getName(), "Double Bed");
        verify(bedTypeRepository, times(1)).findAllByHotelId(1L);
    }

    @Test
    public void testUpdateHotelBeds_SyncFlow() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(sampleHotel));
        
        // Mock existing beds in DB (bedType1 & bedType2)
        List<BedType> existingBeds = new ArrayList<>();
        existingBeds.add(bedType1);
        existingBeds.add(bedType2);
        when(bedTypeRepository.findAllByHotelId(1L)).thenReturn(existingBeds);

        // We receive payload: update bedType1 ("Double Bed"), delete bedType2 ("Single Bed"), insert a new bed type ("Maharani")
        BedTypeRequest req1 = new BedTypeRequest();
        req1.setId("101");
        req1.setName("Updated Double Bed");
        req1.setDescription("Updated desc");
        req1.setCapacity(3);
        req1.setMaxAdults(2);
        req1.setMaxChildren(1);

        BedTypeRequest reqNew = new BedTypeRequest();
        reqNew.setId("bed-temp-123");
        reqNew.setName("Maharani");
        reqNew.setDescription("Maharani bed");
        reqNew.setCapacity(2);
        reqNew.setMaxAdults(2);
        reqNew.setMaxChildren(0);

        List<BedTypeRequest> payload = List.of(req1, reqNew);

        bedTypeService.updateHotelBeds(1L, payload);

        // Verify that bedType2 (ID 102) was deleted because it is not in the payload
        verify(bedTypeRepository, times(1)).deleteAll(anyList());
        
        // Verify saves for req1 and reqNew
        verify(bedTypeRepository, times(2)).save(any(BedType.class));
    }
}
