package com.example.flight.paymentgateway;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayRefundResponse {

    private String refundId;
    private String paymentId;
    private BigDecimal amount;
    private String status;
}