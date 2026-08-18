package com.nag.atithi.services;

import com.nag.atithi.models.BedType;
import com.nag.atithi.requests.BedTypeRequest;

import java.util.List;

public interface BedTypeService {
    List<BedType> getHotelBeds(Long hotelId);
    void updateHotelBeds(Long hotelId, List<BedTypeRequest> bedsList);
}
