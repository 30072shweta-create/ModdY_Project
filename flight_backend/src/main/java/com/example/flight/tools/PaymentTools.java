package main.java.com.example.flight.tools;

import com.example.flight.service.PaymentService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class PaymentTools {

    private final PaymentService paymentService;

    public PaymentTools(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Tool(description = """
            Get live payment information from the database.

            Use this tool when the user asks about:
            - payment status
            - whether a payment was successful
            - whether a payment failed
            - current payment information for a booking

            Use the booking ID or payment reference provided by the user.
            """)
    public Object getPaymentStatus(String bookingReference) {

        System.out.println(
                "PAYMENT TOOL CALLED: " + bookingReference
        );

        // Replace with the exact method from your existing PaymentService
        return paymentService.getPaymentStatus(bookingReference);
    }
}