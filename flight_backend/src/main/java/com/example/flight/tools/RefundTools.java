package main.java.com.example.flight.tools;

import com.example.flight.dto.BookingCancellationResponseDTO;
import com.example.flight.service.BookingCancellationService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class RefundTools {

    private final BookingCancellationService bookingCancellationService;

    public RefundTools(BookingCancellationService bookingCancellationService) {
        this.bookingCancellationService = bookingCancellationService;
    }

    @Tool(description = """
            Get live cancellation and refund information from the database.

            Use this tool when the user asks about:
            - refund status
            - cancellation status
            - refund amount
            - cancellation charge

            The booking ID is required.
            """)
    public BookingCancellationResponseDTO getRefundStatus(Long bookingId) {
        System.out.println("REFUND TOOL CALLED: " + bookingId);

        if (bookingId == null) {
            throw new IllegalArgumentException("Booking ID is required.");
        }

        return bookingCancellationService.getCancellationByBookingId(bookingId);
    }
}
