package com.nag.atithi.responses;

import com.nag.atithi.models.Payment;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@Builder
public class PaymentResponse {
    private Long id;
    private Long bookingId;
    private String paymentMethod;
    private Double totalAmount;
    private Double amountPaid;
    private Double cashAmount;
    private Double onlineAmount;
    private String status;
    private String paymentDate;
    private String transactionId;

    public static PaymentResponse fromEntity(Payment payment) {
        if (payment == null) return null;
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        return PaymentResponse.builder()
                .id(payment.getId())
                .bookingId(payment.getBooking().getId())
                .paymentMethod(payment.getPaymentMethod().name())
                .totalAmount(payment.getTotalAmount())
                .amountPaid(payment.getAmountPaid())
                .cashAmount(payment.getCashAmount())
                .onlineAmount(payment.getOnlineAmount())
                .status(payment.getStatus().name())
                .paymentDate(payment.getPaymentDate() != null ? payment.getPaymentDate().format(formatter) : null)
                .transactionId(payment.getTransactionId())
                .build();
    }
}
