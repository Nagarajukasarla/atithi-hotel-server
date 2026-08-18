package com.nag.atithi.requests;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateAmenitiesRequest {
    
    @NotNull(message = "Amenities list cannot be null")
    private List<String> amenities;
}
