package com.nag.atithi.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BedTypeRequest {
    
    private String id;
    
    @NotBlank(message = "Bed type name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Max adults is required")
    private Integer maxAdults;
    
    @NotNull(message = "Max children is required")
    private Integer maxChildren;
    
    @NotNull(message = "Capacity is required")
    private Integer capacity;
}
