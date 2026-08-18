package com.nag.atithi.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequest {
    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    @NotNull(message = "Original amount is required")
    private Double totalAmount;

    @NotNull(message = "Amount paid is required")
    private Double amountPaid;

    @NotBlank(message = "Status is required")
    private String status;

    private String transactionId;
}
