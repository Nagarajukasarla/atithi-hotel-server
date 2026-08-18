package com.nag.atithi.services;

import java.util.List;

public interface AmenityService {
    List<String> getHotelAmenities(Long hotelId);
    void updateHotelAmenities(Long hotelId, List<String> amenityCodes);
}
