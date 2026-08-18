package com.nag.atithi.implementations;

import com.nag.atithi.models.Amenity;
import com.nag.atithi.models.Hotel;
import com.nag.atithi.repositories.AmenityRepository;
import com.nag.atithi.repositories.HotelRepository;
import com.nag.atithi.services.AmenityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AmenityServiceImpl implements AmenityService {

    private final HotelRepository hotelRepository;
    private final AmenityRepository amenityRepository;

    @Override
    @Transactional(readOnly = true)
    public List<String> getHotelAmenities(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Hotel not found with id: " + hotelId));
        
        return hotel.getAmenities().stream()
                .map(Amenity::getCode)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateHotelAmenities(Long hotelId, List<String> amenityCodes) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Hotel not found with id: " + hotelId));

        List<Amenity> matchingAmenities = amenityRepository.findAllByCodeIn(amenityCodes);
        
        // Clear previous associations and apply the new set
        hotel.getAmenities().clear();
        hotel.getAmenities().addAll(new HashSet<>(matchingAmenities));

        hotelRepository.save(hotel);
    }
}
