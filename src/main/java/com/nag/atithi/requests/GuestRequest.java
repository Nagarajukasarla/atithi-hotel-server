package com.nag.atithi.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class GuestRequest {
    @NotBlank(message = "Guest name is required")
    private String name;

    @NotBlank(message = "Aadhaar number is required")
    @Pattern(regexp = "^\\d{12}$", message = "Aadhaar number must be exactly 12 digits")
    private String aadhaarNumber;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^\\d{10,15}$", message = "Mobile number must be between 10 and 15 digits")
    private String mobileNumber;

    private Integer age;
    private String address;
}
