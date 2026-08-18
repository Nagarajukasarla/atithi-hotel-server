package com.nag.atithi.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import com.nag.atithi.enums.PremiumType;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class RoomTypeRequest {
    
    @NotBlank(message = "Room type name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Base price is required")
    private BigDecimal basePrice;
    
    @NotNull(message = "Max adults is required")
    private Integer maxAdults;
    
    @NotNull(message = "Max children is required")
    private Integer maxChildren;
    
    @NotNull(message = "Max occupancy is required")
    private Integer maxOccupancy;
    
    private String bedType;
    
    private String size;
    
    private List<String> amenityIds;
    
    @NotBlank(message = "Status is required")
    private String status;

    private Boolean isPremium;
    private PremiumType premiumType;
}
