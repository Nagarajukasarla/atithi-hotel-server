package com.nag.atithi.services;

import com.nag.atithi.requests.PaymentRequest;
import com.nag.atithi.responses.PaymentResponse;

public interface PaymentService {
    PaymentResponse createOrUpdatePayment(Long bookingId, PaymentRequest request);
    PaymentResponse getPaymentByBookingId(Long bookingId);
}
