package main.java.com.example.flight.tools;

import com.example.flight.service.BookingService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class BookingTools {

    private final BookingService bookingService;

    public BookingTools(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Tool(description = """
            Get live booking information from the database.

            Use this tool when the user asks about:
            - booking status
            - booking details
            - current booking information

            The booking reference or booking ID is required
            to retrieve a specific booking.
            """)
    public Object getBookingDetails(String bookingReference) {

        System.out.println(
                "BOOKING TOOL CALLED: " + bookingReference
        );

        if (bookingReference == null || bookingReference.isBlank()) {
            throw new IllegalArgumentException("Booking reference or booking ID is required.");
        }

        String reference = bookingReference.trim();
        if (reference.matches("\\d+")) {
            return bookingService.getBookingById(Long.parseLong(reference));
        }

        return bookingService.getBookingByCode(reference);
    }
}
