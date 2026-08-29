package com.example.flight.paymentgateway.razorpay;

import java.math.BigDecimal;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.flight.paymentgateway.GatewayOrderResponse;
import com.example.flight.paymentgateway.GatewayRefundResponse;
import com.example.flight.paymentgateway.PaymentGateway;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Refund;
import com.razorpay.Utils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RazorpayPaymentGateway implements PaymentGateway {

    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;


    @Override
    public GatewayOrderResponse createOrder(
            BigDecimal amount,
            String currency,
            String receipt
    ) {

        try {

            long amountInPaise = amount
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            JSONObject options = new JSONObject();

            options.put("amount", amountInPaise);
            options.put("currency", currency);
            options.put("receipt", receipt);

            Order order = razorpayClient.orders.create(options);

            return GatewayOrderResponse.builder()
                    .orderId(order.get("id"))
                    .amount(amount)
                    .currency(currency)
                    .status(order.get("status"))
                    .build();

        } catch (Exception exception) {

            throw new RuntimeException(
                    "Failed to create Razorpay order",
                    exception
            );
        }
    }


    @Override
    public boolean verifyPayment(
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature
    ) {

        try {

            JSONObject options = new JSONObject();

            options.put(
                    "razorpay_order_id",
                    razorpayOrderId
            );

            options.put(
                    "razorpay_payment_id",
                    razorpayPaymentId
            );

            options.put(
                    "razorpay_signature",
                    razorpaySignature
            );

            return Utils.verifyPaymentSignature(
                    options,
                    razorpayKeySecret
            );

        } catch (Exception exception) {

            return false;
        }
    }


    @Override
    public GatewayRefundResponse refundPayment(
            String razorpayPaymentId,
            BigDecimal refundAmount
    ) {

        try {

            long amountInPaise = refundAmount
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            JSONObject options = new JSONObject();

            options.put(
                    "amount",
                    amountInPaise
            );

            Refund refund = razorpayClient
                    .payments
                    .refund(
                            razorpayPaymentId,
                            options
                    );

            return GatewayRefundResponse
                    .builder()
                    .refundId(
                            refund.get("id")
                    )
                    .paymentId(
                            razorpayPaymentId
                    )
                    .amount(
                            refundAmount
                    )
                    .status(
                            refund.get("status")
                    )
                    .build();

        } catch (Exception exception) {

            throw new RuntimeException(
                    "Failed to process Razorpay refund",
                    exception
            );
        }
    }
}