package com.nag.atithi.implementations;

import com.nag.atithi.models.BedType;
import com.nag.atithi.models.Hotel;
import com.nag.atithi.repositories.BedTypeRepository;
import com.nag.atithi.repositories.HotelRepository;
import com.nag.atithi.requests.BedTypeRequest;
import com.nag.atithi.services.BedTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BedTypeServiceImpl implements BedTypeService {

    private final HotelRepository hotelRepository;
    private final BedTypeRepository bedTypeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BedType> getHotelBeds(Long hotelId) {
        return bedTypeRepository.findAllByHotelId(hotelId);
    }

    @Override
    @Transactional
    public void updateHotelBeds(Long hotelId, List<BedTypeRequest> bedsList) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Hotel not found with id: " + hotelId));

        // Fetch currently saved bed types for the hotel
        List<BedType> existingBeds = bedTypeRepository.findAllByHotelId(hotelId);
        Map<Long, BedType> existingBedsMap = existingBeds.stream()
                .collect(Collectors.toMap(BedType::getId, b -> b));

        // Process request items
        Set<Long> idsToKeep = bedsList.stream()
                .filter(req -> req.getId() != null && !req.getId().startsWith("bed-"))
                .map(req -> Long.valueOf(req.getId()))
                .collect(Collectors.toSet());

        // 1. Delete items not present in the new payload list
        List<BedType> bedsToDelete = existingBeds.stream()
                .filter(b -> !idsToKeep.contains(b.getId()))
                .collect(Collectors.toList());
        bedTypeRepository.deleteAll(bedsToDelete);

        // 2. Save new or update existing configurations
        for (BedTypeRequest req : bedsList) {
            BedType bedType;
            if (req.getId() != null && !req.getId().startsWith("bed-")) {
                // Update
                Long id = Long.valueOf(req.getId());
                bedType = existingBedsMap.get(id);
                if (bedType == null) {
                    throw new RuntimeException("Bed type not found for id: " + id);
                }
            } else {
                // Insert New
                bedType = new BedType();
                bedType.setHotel(hotel);
            }

            bedType.setName(req.getName().trim());
            bedType.setDescription(req.getDescription() != null ? req.getDescription().trim() : null);
            bedType.setMaxAdults(req.getMaxAdults());
            bedType.setMaxChildren(req.getMaxChildren());
            bedType.setCapacity(req.getCapacity());

            bedTypeRepository.save(bedType);
        }
    }
}
