package com.nag.atithi.implementations;

import com.nag.atithi.enums.RoomTypeStatus;
import com.nag.atithi.models.Amenity;
import com.nag.atithi.models.Hotel;
import com.nag.atithi.models.RoomType;
import com.nag.atithi.repositories.AmenityRepository;
import com.nag.atithi.repositories.HotelRepository;
import com.nag.atithi.repositories.RoomTypeRepository;
import com.nag.atithi.repositories.RoomRepository;
import com.nag.atithi.enums.RoomStatus;
import com.nag.atithi.requests.RoomTypeRequest;
import com.nag.atithi.responses.RoomTypeResponse;
import com.nag.atithi.services.RoomTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.nag.atithi.enums.PremiumType;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomTypeServiceImpl implements RoomTypeService {

    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final AmenityRepository amenityRepository;
    private final RoomRepository roomRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RoomTypeResponse> getHotelRoomTypes(Long hotelId) {
        List<RoomType> roomTypes = roomTypeRepository.findAllByHotelId(hotelId);
        
        List<Object[]> counts = roomRepository.countRoomsByRoomTypeAndStatus(hotelId, RoomStatus.VACANT);
        Map<Long, int[]> countsMap = new HashMap<>();
        for (Object[] row : counts) {
            Long roomTypeId = (Long) row[0];
            Long total = (Long) row[1];
            Long active = (Long) row[2];
            countsMap.put(roomTypeId, new int[]{active.intValue(), total.intValue()});
        }

        return roomTypes.stream()
                .map(rt -> {
                    int[] c = countsMap.getOrDefault(rt.getId(), new int[]{0, 0});
                    return RoomTypeResponse.fromEntity(rt, c[0], c[1]);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RoomTypeResponse createRoomType(Long hotelId, RoomTypeRequest request) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Hotel not found with id: " + hotelId));

        RoomType roomType = RoomType.builder()
                .hotel(hotel)
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .basePrice(request.getBasePrice())
                .maxAdults(request.getMaxAdults())
                .maxChildren(request.getMaxChildren())
                .maxOccupancy(request.getMaxOccupancy())
                .bedType(request.getBedType() != null ? request.getBedType().trim() : null)
                .size(request.getSize() != null ? request.getSize().trim() : null)
                .status(RoomTypeStatus.valueOf(request.getStatus()))
                .isPremium(request.getIsPremium() != null ? request.getIsPremium() : false)
                .premiumType(Boolean.TRUE.equals(request.getIsPremium()) ? request.getPremiumType() : null)
                .isHourly(request.getIsHourly() != null ? request.getIsHourly() : false)
                .hourlyPrice(Boolean.TRUE.equals(request.getIsHourly()) ? request.getHourlyPrice() : null)
                .build();

        if (request.getAmenityIds() != null && !request.getAmenityIds().isEmpty()) {
            List<Amenity> amenities = amenityRepository.findAllByCodeIn(request.getAmenityIds());
            roomType.setAmenities(new HashSet<>(amenities));
        }

        RoomType saved = roomTypeRepository.save(roomType);
        return RoomTypeResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public RoomTypeResponse updateRoomType(Long hotelId, Long id, RoomTypeRequest request) {
        RoomType roomType = roomTypeRepository.findByHotelIdAndId(hotelId, id)
                .orElseThrow(() -> new RuntimeException("Room type not found with id: " + id));

        roomType.setName(request.getName().trim());
        roomType.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        roomType.setBasePrice(request.getBasePrice());
        roomType.setMaxAdults(request.getMaxAdults());
        roomType.setMaxChildren(request.getMaxChildren());
        roomType.setMaxOccupancy(request.getMaxOccupancy());
        roomType.setBedType(request.getBedType() != null ? request.getBedType().trim() : null);
        roomType.setSize(request.getSize() != null ? request.getSize().trim() : null);
        roomType.setStatus(RoomTypeStatus.valueOf(request.getStatus()));

        if (request.getIsPremium() != null) {
            roomType.setIsPremium(request.getIsPremium());
        }
        if (Boolean.TRUE.equals(roomType.getIsPremium())) {
            roomType.setPremiumType(request.getPremiumType());
        } else {
            roomType.setPremiumType(null); // Clean up if no longer premium
        }

        if (request.getIsHourly() != null) {
            roomType.setIsHourly(request.getIsHourly());
        }
        if (Boolean.TRUE.equals(roomType.getIsHourly())) {
            roomType.setHourlyPrice(request.getHourlyPrice());
        } else {
            roomType.setHourlyPrice(null); // Clean up if no longer hourly
        }

        if (request.getAmenityIds() != null) {
            List<Amenity> amenities = amenityRepository.findAllByCodeIn(request.getAmenityIds());
            roomType.getAmenities().clear();
            roomType.getAmenities().addAll(amenities);
        } else {
            roomType.getAmenities().clear();
        }

        RoomType saved = roomTypeRepository.save(roomType);
        return RoomTypeResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void deleteRoomType(Long hotelId, Long id) {
        RoomType roomType = roomTypeRepository.findByHotelIdAndId(hotelId, id)
                .orElseThrow(() -> new RuntimeException("Room type not found with id: " + id));
        roomTypeRepository.delete(roomType);
    }
}
