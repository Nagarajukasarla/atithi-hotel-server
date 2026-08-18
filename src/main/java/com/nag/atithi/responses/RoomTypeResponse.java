package com.nag.atithi.responses;

import com.nag.atithi.models.Amenity;
import com.nag.atithi.models.RoomType;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public record RoomTypeResponse(
        Long id,
        String name,
        String description,
        BigDecimal basePrice,
        Integer maxAdults,
        Integer maxChildren,
        Integer maxOccupancy,
        String bedType,
        String size,
        String status,
        List<String> amenityIds,
        Boolean isPremium,
        String premiumType
) {
    public static RoomTypeResponse fromEntity(RoomType entity) {
        List<String> amenityIds = entity.getAmenities().stream()
                .map(Amenity::getCode)
                .collect(Collectors.toList());

        return new RoomTypeResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getBasePrice(),
                entity.getMaxAdults(),
                entity.getMaxChildren(),
                entity.getMaxOccupancy(),
                entity.getBedType(),
                entity.getSize(),
                entity.getStatus().name(),
                amenityIds,
                entity.getIsPremium(),
                entity.getPremiumType() != null ? entity.getPremiumType().name() : null
        );
    }
}
