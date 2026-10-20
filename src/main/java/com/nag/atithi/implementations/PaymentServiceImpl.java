package com.nag.atithi.implementations;

import com.nag.atithi.enums.PaymentMethod;
import com.nag.atithi.enums.PaymentStatus;
import com.nag.atithi.models.Booking;
import com.nag.atithi.models.Payment;
import com.nag.atithi.repositories.BookingRepository;
import com.nag.atithi.repositories.PaymentRepository;
import com.nag.atithi.requests.PaymentRequest;
import com.nag.atithi.responses.PaymentResponse;
import com.nag.atithi.services.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
    public PaymentResponse createOrUpdatePayment(Long bookingId, PaymentRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElse(null);

        if (payment == null) {
            payment = Payment.builder()
                    .booking(booking)
                    .build();
        }

        Double cash = request.getCashAmount();
        Double online = request.getOnlineAmount();

        PaymentMethod method;
        if (cash != null || online != null) {
            double c = cash != null ? cash : 0.0;
            double o = online != null ? online : 0.0;
            if (c > 0 && o > 0) {
                method = PaymentMethod.SPLIT;
            } else if (o > 0) {
                method = PaymentMethod.ONLINE;
            } else {
                method = PaymentMethod.CASH;
            }
            payment.setCashAmount(c);
            payment.setOnlineAmount(o);
        } else {
            method = PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());
            if (method == PaymentMethod.ONLINE) {
                payment.setCashAmount(0.0);
                payment.setOnlineAmount(request.getAmountPaid());
            } else {
                payment.setCashAmount(request.getAmountPaid());
                payment.setOnlineAmount(0.0);
            }
        }

        payment.setPaymentMethod(method);
        payment.setTotalAmount(request.getTotalAmount());
        payment.setAmountPaid(request.getAmountPaid());
        payment.setStatus(PaymentStatus.valueOf(request.getStatus().toUpperCase()));
        payment.setTransactionId(request.getTransactionId());
        payment.setPaymentDate(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);
        return PaymentResponse.fromEntity(savedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBookingId(Long bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for booking id: " + bookingId));
        return PaymentResponse.fromEntity(payment);
    }
}
