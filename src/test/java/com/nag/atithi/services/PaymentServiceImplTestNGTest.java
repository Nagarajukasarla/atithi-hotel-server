package com.nag.atithi.services;

import com.nag.atithi.enums.PaymentMethod;
import com.nag.atithi.enums.PaymentStatus;
import com.nag.atithi.implementations.PaymentServiceImpl;
import com.nag.atithi.models.Booking;
import com.nag.atithi.models.Payment;
import com.nag.atithi.repositories.BookingRepository;
import com.nag.atithi.repositories.PaymentRepository;
import com.nag.atithi.requests.PaymentRequest;
import com.nag.atithi.responses.PaymentResponse;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PaymentServiceImplTestNGTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private AutoCloseable mocks;
    private Booking sampleBooking;
    private Payment samplePayment;

    @BeforeClass
    public void setUpClass() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterClass
    public void tearDownClass() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @BeforeMethod
    public void setUp() {
        Mockito.reset(paymentRepository, bookingRepository);

        sampleBooking = Booking.builder()
                .id(1001L)
                .totalPrice(1500.0)
                .checkIn(LocalDateTime.now())
                .checkOut(LocalDateTime.now().plusDays(1))
                .isHourly(false)
                .build();

        samplePayment = Payment.builder()
                .id(5001L)
                .booking(sampleBooking)
                .paymentMethod(PaymentMethod.CASH)
                .totalAmount(1500.0)
                .amountPaid(1500.0)
                .status(PaymentStatus.COMPLETED)
                .paymentDate(LocalDateTime.now())
                .build();
    }

    @Test
    public void testCreatePayment_New_Success() {
        when(bookingRepository.findById(1001L)).thenReturn(Optional.of(sampleBooking));
        when(paymentRepository.findByBookingId(1001L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(5001L);
            return p;
        });

        PaymentRequest request = new PaymentRequest();
        request.setPaymentMethod("CASH");
        request.setTotalAmount(1500.0);
        request.setAmountPaid(1500.0);
        request.setStatus("COMPLETED");

        PaymentResponse response = paymentService.createOrUpdatePayment(1001L, request);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getId(), Long.valueOf(5001L));
        Assert.assertEquals(response.getBookingId(), Long.valueOf(1001L));
        Assert.assertEquals(response.getPaymentMethod(), "CASH");
        Assert.assertEquals(response.getTotalAmount(), 1500.0);
        Assert.assertEquals(response.getAmountPaid(), 1500.0);
        Assert.assertEquals(response.getStatus(), "COMPLETED");

        verify(bookingRepository, times(1)).findById(1001L);
        verify(paymentRepository, times(1)).findByBookingId(1001L);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    public void testUpdatePayment_Existing_Success() {
        when(bookingRepository.findById(1001L)).thenReturn(Optional.of(sampleBooking));
        when(paymentRepository.findByBookingId(1001L)).thenReturn(Optional.of(samplePayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentRequest request = new PaymentRequest();
        request.setPaymentMethod("ONLINE");
        request.setTotalAmount(1500.0);
        request.setAmountPaid(1000.0); // Partial payment
        request.setStatus("PENDING");
        request.setTransactionId("TXN12345");

        PaymentResponse response = paymentService.createOrUpdatePayment(1001L, request);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getId(), Long.valueOf(5001L));
        Assert.assertEquals(response.getPaymentMethod(), "ONLINE");
        Assert.assertEquals(response.getAmountPaid(), 1000.0);
        Assert.assertEquals(response.getStatus(), "PENDING");
        Assert.assertEquals(response.getTransactionId(), "TXN12345");

        verify(bookingRepository, times(1)).findById(1001L);
        verify(paymentRepository, times(1)).findByBookingId(1001L);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    public void testGetPaymentByBookingId_Success() {
        when(paymentRepository.findByBookingId(1001L)).thenReturn(Optional.of(samplePayment));

        PaymentResponse response = paymentService.getPaymentByBookingId(1001L);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getId(), Long.valueOf(5001L));
        Assert.assertEquals(response.getBookingId(), Long.valueOf(1001L));

        verify(paymentRepository, times(1)).findByBookingId(1001L);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testGetPaymentByBookingId_NotFound_ThrowsException() {
        when(paymentRepository.findByBookingId(1001L)).thenReturn(Optional.empty());

        paymentService.getPaymentByBookingId(1001L);
    }
}
