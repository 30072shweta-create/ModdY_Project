package com.example.flight.paymentgateway;

import java.math.BigDecimal;

public interface PaymentGateway {

    GatewayOrderResponse createOrder(
            BigDecimal amount,
            String currency,
            String receipt
    );

    boolean verifyPayment(
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature
    );

    GatewayRefundResponse refundPayment(
            String razorpayPaymentId,
            BigDecimal refundAmount
    );
}