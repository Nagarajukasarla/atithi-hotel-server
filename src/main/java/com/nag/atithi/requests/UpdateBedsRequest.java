package com.nag.atithi.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateBedsRequest {
    
    @NotNull(message = "Beds list cannot be null")
    @Valid
    private List<BedTypeRequest> beds;
}
