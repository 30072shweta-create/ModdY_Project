package com.example.flight.model;

public enum Domain {

    FLIGHTS_AND_SEATS(
            "Flights and Seats",
            "flights-and-seats.md",
            "flights-andseats-faq.md"
    ),

    BOOKING(
            "Booking",
            "booking.md",
            "booking-faq.md"
    ),

    PAYMENT(
            "Payment",
            "payment.md",
            "payment-faq.md"
    ),

    CANCELLATION_AND_REFUND(
            "Cancellation and Refund",
            "cancellation-and-refund.md",
            "cancellation-and-refund-faq.md"
    ),

    PRICING_AND_COUPONS(
            "Pricing and Coupons",
            "pricing-and-cupon.md",
            "pricing-and-coupan-faq.md"
    );

    private final String displayName;
    private final String fileName;
    private final String faqFileName;

    Domain(String displayName, String fileName, String faqFileName) {
        this.displayName = displayName;
        this.fileName = fileName;
        this.faqFileName = faqFileName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFaqFileName() {
        return faqFileName;
    }
}
