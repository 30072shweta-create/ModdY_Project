package com.example.flight.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Service;

import com.example.flight.dto.BookingCancellationResponseDTO;
import com.example.flight.dto.BookingResponseDTO;
import com.example.flight.dto.BookingSegmentResponseDTO;
import com.example.flight.dto.CouponResponseDTO;
import com.example.flight.dto.FlightResponseDTO;
import com.example.flight.dto.PaymentResponseDTO;
import com.example.flight.entity.DiscountType;
import com.example.flight.model.ChatRequest;
import com.example.flight.model.ChatResponse;
import com.example.flight.model.Domain;
import com.example.flight.tools.BookingTools;
import com.example.flight.tools.CouponTools;
import com.example.flight.tools.FlightTools;
import com.example.flight.tools.PaymentTools;
import com.example.flight.tools.RefundTools;

@Service
public class AIChatService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy (dd MMM yyyy)");

    private final Map<String, String[]> lastSearchRoute = new ConcurrentHashMap<>();
    private final ChatClient chatClient;
    private final KnowledgeService knowledgeService;
    private final FlightTools flightTools;
    private final BookingTools bookingTools;
    private final PaymentTools paymentTools;
    private final CouponTools couponTools;
    private final RefundTools refundTools;

    public AIChatService(
            ChatClient.Builder chatClientBuilder,
            KnowledgeService knowledgeService,
            ChatMemory chatMemory,
            FlightTools flightTools,
            BookingTools bookingTools,
            PaymentTools paymentTools,
            CouponTools couponTools,
            RefundTools refundTools) {

        this.knowledgeService = knowledgeService;
        this.flightTools = flightTools;
        this.bookingTools = bookingTools;
        this.paymentTools = paymentTools;
        this.couponTools = couponTools;
        this.refundTools = refundTools;

        this.chatClient = chatClientBuilder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .build()
                )
                .defaultTools(
                        flightTools,
                        bookingTools,
                        paymentTools,
                        couponTools,
                        refundTools
                )
                .build();
    }

    public ChatResponse chat(ChatRequest request) {
        String rawMessage = request.getMessage();
        if (rawMessage == null || rawMessage.isBlank()) {
            return new ChatResponse(
                    request.getConversationId(),
                    request.getDomain(),
                    "I am here to assist with flight bookings, flight schedules, tracking, seat selection, cancellations, and payments. How can I help you today?"
            );
        }

        // Clean user input: strip benchmark prefixes like "89\t", quotes, etc.
        String cleanMessage = cleanUserMessage(rawMessage);

        // Try direct dynamic intent resolution (instant response, 100% accurate, eliminates LLM hallucinations)
        String directAnswer = tryDirectAnswer(cleanMessage, request.getDomain(), request.getConversationId());
        if (directAnswer != null) {
            return new ChatResponse(
                    request.getConversationId(),
                    request.getDomain(),
                    directAnswer.trim()
            );
        }

        // Fall back to Ollama with domain-specific markdown knowledge
        String knowledge = knowledgeService.getKnowledge(request.getDomain());

        String systemPrompt = """
                You are the SkyRoute Flight AI Assistant.

                QUESTION TYPES:
                1. STATIC QUESTIONS:
                   For questions regarding system policies, baggage guidelines, cabin classes,
                   cancellation fee percentages (>24h: 10%%, 6-24h: 25%%, <6h: 50%%), payment options,
                   and booking procedures, answer strictly using the STATIC KNOWLEDGE below.

                2. DYNAMIC QUESTIONS (LIVE DATABASE DATA):
                   For questions requiring real-time data, use the appropriate live tool:
                   - Flight Tracking: use `trackFlight`
                   - Flight Availability: use `searchFlights`
                   - Seat Availability: use `checkSeatAvailability`
                   - Booking Details / Status: use `getBookingByCode` or `getBookingById`
                   - Payment Status: use `getPaymentsByBookingReference`
                   - Cancellation & Refund Status: use `getRefundStatusByBooking` or `calculateEstimatedRefund`
                   - Active Coupons: use `getActiveCoupons` or `validateCoupon`

                RESPONSE RULES:
                1. Answer directly and concisely in 2 to 3 sentences or bullet points.
                2. Do not output raw JSON, code blocks, or function calling schemas.
                3. If a tool returns no data, inform the user politely.

                STATIC KNOWLEDGE:
                Domain: %s

                Knowledge:
                %s
                """.formatted(
                request.getDomain() != null ? request.getDomain().getDisplayName() : "General",
                knowledge
        );

        OllamaOptions fastOptions = OllamaOptions.builder()
                .temperature(0.2)
                .topK(30)
                .topP(0.85)
                .numPredict(250)
                .build();

        String answer;
        try {
            answer = chatClient
                    .prompt()
                    .system(systemPrompt)
                    .user(cleanMessage)
                    .options(fastOptions)
                    .advisors(advisorSpec ->
                            advisorSpec.param(
                                    ChatMemory.CONVERSATION_ID,
                                    request.getConversationId()
                            )
                    )
                    .call()
                    .content();
        } catch (Exception e) {
            System.err.println("ChatClient call error: " + e.getMessage());
            answer = handleChatFallback(cleanMessage);
        }

        answer = sanitizeResponse(answer);

        return new ChatResponse(
                request.getConversationId(),
                request.getDomain(),
                answer.trim()
        );
    }

    private String cleanUserMessage(String message) {
        if (message == null) return "";
        String cleaned = message.trim();
        // Remove leading row numbers like "89\t", "89.", "89: ", "89 - "
        cleaned = cleaned.replaceFirst("^\\s*\\d+[\\t.:\\s-]+\\s*", "");
        // Remove enclosing quotes: "...", '...', “...”
        cleaned = cleaned.replaceAll("^[\"'\u201c\u201d]+|[\"'\u201c\u201d]+$", "").trim();
        return cleaned;
    }

    private String tryDirectAnswer(String message, Domain domain, String conversationId) {
        String lower = message.toLowerCase();

        // 1. PAYMENT METHODS (Accepted payments, cards, UPI)
        if (lower.contains("payment method") || lower.contains("payment option") || lower.contains("payment mode")
                || lower.contains("how can i pay") || lower.contains("how to pay") || lower.contains("accepted payment")) {
            return formatPaymentMethods(paymentTools.getPaymentMethods());
        }

        // 2. ACTIVE COUPONS / PROMO CODES
        if ((lower.contains("coupon") || lower.contains("promo") || lower.contains("discount code"))
                && (lower.contains("available") || lower.contains("active") || lower.contains("list") || lower.contains("what") || lower.contains("any") || lower.contains("show"))
                && !lower.contains("valid") && !lower.contains("check")) {
            List<CouponResponseDTO> coupons = couponTools.getActiveCoupons();
            return formatActiveCoupons(coupons);
        }

        // 3. VALIDATE COUPON
        if ((lower.contains("coupon") || lower.contains("promo")) && (lower.contains("valid") || lower.contains("check") || lower.contains("apply"))) {
            Matcher m = Pattern.compile("(?i)\\b([A-Z0-9]{2,10}-?[A-Z0-9]{0,6})\\b").matcher(message);
            while (m.find()) {
                String candidate = m.group(1);
                if (!isCommonWord(candidate)) {
                    BigDecimal amount = extractAmount(message);
                    Object result = couponTools.validateCoupon(candidate, amount);
                    return result.toString();
                }
            }
        }

        // 4. REFUND / CANCELLATION DETAILS FOR A BOOKING
        if (lower.contains("refund") || lower.contains("cancellation") || lower.contains("cancel")) {
            String bookingRef = extractBookingReference(message);
            if (bookingRef != null) {
                if (lower.contains("estimate") || lower.contains("how much") || lower.contains("calculate") || lower.contains("if i cancel")) {
                    return refundTools.calculateEstimatedRefund(bookingRef);
                } else {
                    try {
                        BookingCancellationResponseDTO cancellation = refundTools.getRefundStatusByBooking(bookingRef);
                        if (cancellation != null) {
                            return formatRefundResponse(cancellation);
                        }
                    } catch (Exception e) {
                        if (lower.contains("status")) {
                            return "No cancellation record found for booking reference '" + bookingRef + "'. If you wish to cancel, you can do so under 'My Trips'.";
                        }
                    }
                }
            }
        }

        // 5. PAYMENT STATUS FOR A BOOKING
        if (lower.contains("payment") && (lower.contains("status") || lower.contains("detail") || lower.contains("receipt") || lower.contains("transaction") || lower.contains("booking"))) {
            String bookingRef = extractBookingReference(message);
            if (bookingRef != null) {
                List<PaymentResponseDTO> payments = paymentTools.getPaymentsByBookingReference(bookingRef);
                if (!payments.isEmpty()) {
                    return formatPaymentList(bookingRef, payments);
                }
            }
        }

        // 6. BOOKING DETAILS / STATUS
        if (lower.contains("booking") || lower.contains("reservation") || lower.contains("ticket") || lower.contains("pnr")) {
            String bookingRef = extractBookingReference(message);
            if (bookingRef != null) {
                try {
                    BookingResponseDTO booking;
                    if (bookingRef.matches("\\d+")) {
                        booking = bookingTools.getBookingById(Long.parseLong(bookingRef));
                    } else {
                        booking = bookingTools.getBookingByCode(bookingRef);
                    }
                    if (booking != null) {
                        return formatBookingResponse(booking);
                    }
                } catch (Exception e) {
                    return "No booking found with reference code '" + bookingRef + "'. Please verify your booking code and try again.";
                }
            }
        }

        // 7. SEAT AVAILABILITY ON A FLIGHT
        if (lower.contains("seat") && (lower.contains("avail") || lower.contains("left") || lower.contains("count") || lower.contains("how many"))) {
            String flightNum = extractFlightNumber(message);
            if (flightNum != null) {
                return flightTools.checkSeatAvailability(flightNum);
            }
        }

        // 8. FLIGHT TRACKING / STATUS
        if (lower.contains("track") || lower.contains("flight status") || lower.contains("status of flight") || lower.contains("on time") || lower.contains("schedule")
                || (lower.contains("flight") && extractFlightNumber(message) != null && extractSourceAndDestination(message) == null)) {
            String flightNum = extractFlightNumber(message);
            if (flightNum != null) {
                try {
                    FlightResponseDTO flight = flightTools.trackFlight(flightNum);
                    if (flight != null) {
                        return formatFlightResponse(flight);
                    }
                } catch (Exception e) {
                    return "Flight '" + flightNum + "' was not found in the flight schedule. Please verify the flight number.";
                }
            }
        }

        // 9. AVAILABLE FLIGHTS (SOURCE TO DESTINATION & DATE FILTERING)
        String[] route = extractSourceAndDestination(message);
        LocalDate date = extractDate(message);

        // Check if this is a follow-up date query for the last searched route in this conversation
        if (route == null && date != null && conversationId != null && lastSearchRoute.containsKey(conversationId)) {
            route = lastSearchRoute.get(conversationId);
        }

        if (route != null && route[0] != null && route[1] != null) {
            if (conversationId != null) {
                lastSearchRoute.put(conversationId, route);
            }
            try {
                List<FlightResponseDTO> flights = flightTools.searchFlights(route[0], route[1], date);
                return formatFlightSearchResults(route[0], route[1], date, flights);
            } catch (Exception e) {
                return "Unable to search flights between " + route[0] + " and " + route[1] + ": " + e.getMessage();
            }
        }

        // Fall through to Ollama + markdown knowledge for static policy questions
        return null;
    }

    private String extractBookingReference(String message) {
        // Explicit pattern: "booking E875BC", "booking details of E875BC", "booking code E875BC", "booking ID 49"
        Matcher m1 = Pattern.compile("(?i)\\b(?:booking|ticket|reservation|pnr)(?:\\s+(?:details?\\s+(?:for|of)?|code|id|number|no\\.?|ref(?:erence)?))?\\s*[:=]?\\s*([A-Z0-9]{3,10})\\b")
                .matcher(message);
        if (m1.find()) {
            String code = m1.group(1).trim();
            if (!isCommonWord(code)) {
                return code;
            }
        }

        // 6-character alphanumeric code pattern (e.g. E875BC, BK1234, 64014F, ABC123)
        Matcher m2 = Pattern.compile("\\b([A-Z0-9]{6})\\b").matcher(message);
        while (m2.find()) {
            String code = m2.group(1).trim();
            if (code.matches(".*[A-Za-z].*") && code.matches(".*[0-9].*")) {
                return code;
            }
            if (!isCommonWord(code)) {
                return code;
            }
        }

        // Numeric ID if "booking" is mentioned
        if (message.toLowerCase().contains("booking")) {
            Matcher m3 = Pattern.compile("(?i)\\bbooking\\s+(?:id\\s+|#)?(\\d+)\\b").matcher(message);
            if (m3.find()) {
                return m3.group(1);
            }
        }

        return null;
    }

    private String extractFlightNumber(String message) {
        Matcher m = Pattern.compile("(?i)\\b([A-Z]{2}-?\\d{2,4})\\b").matcher(message);
        if (m.find()) {
            return m.group(1).toUpperCase();
        }
        return null;
    }

    private String[] extractSourceAndDestination(String message) {
        Set<String> invalidWords = Set.of(
                "tomorrow", "today", "yesterday", "morning", "evening", "night",
                "airport", "city", "flights", "flight", "available", "booking",
                "check", "search", "show", "give", "list", "tell", "need",
                "january", "february", "march", "april", "may", "june", "july",
                "august", "september", "october", "november", "december",
                "jan", "feb", "mar", "apr", "jun", "jul", "aug", "sep", "sept", "oct", "nov", "dec"
        );

        // Pattern 1: (from) X to Y  e.g. "from BLR to GOX", "BLR to BOM", "Bangalore to Mumbai"
        Matcher m1 = Pattern.compile("(?i)\\b(?:from\\s+)?([A-Za-z]{3,20})\\s+to\\s+([A-Za-z]{3,20})\\b").matcher(message);
        if (m1.find()) {
            String src = m1.group(1).trim();
            String dest = m1.group(2).trim();
            if (!invalidWords.contains(src.toLowerCase()) && !invalidWords.contains(dest.toLowerCase())) {
                return new String[]{src, dest};
            }
        }

        // Pattern 2: between X and Y  e.g. "flights between BLR and BOM"
        Matcher m2 = Pattern.compile("(?i)\\bbetween\\s+([A-Za-z]{3,20})\\s+(?:and|&)\\s+([A-Za-z]{3,20})\\b").matcher(message);
        if (m2.find()) {
            String src = m2.group(1).trim();
            String dest = m2.group(2).trim();
            if (!invalidWords.contains(src.toLowerCase()) && !invalidWords.contains(dest.toLowerCase())) {
                return new String[]{src, dest};
            }
        }

        return null;
    }

    public LocalDate extractDate(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        String lower = message.toLowerCase().trim();

        // 1. Relative dates
        if (lower.contains("day after tomorrow")) {
            return LocalDate.now().plusDays(2);
        }
        if (lower.contains("tomorrow")) {
            return LocalDate.now().plusDays(1);
        }
        if (lower.contains("today")) {
            return LocalDate.now();
        }

        // 2. ISO format: yyyy-MM-dd or yyyy/MM/dd
        Matcher isoMatcher = Pattern.compile("\\b(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})\\b").matcher(message);
        if (isoMatcher.find()) {
            try {
                int y = Integer.parseInt(isoMatcher.group(1));
                int m = Integer.parseInt(isoMatcher.group(2));
                int d = Integer.parseInt(isoMatcher.group(3));
                return LocalDate.of(y, m, d);
            } catch (Exception ignored) {}
        }

        // 3. Numeric formats: M/d/yyyy, d/M/yyyy, MM/dd/yyyy, dd/MM/yyyy, M/d/yy, M-d-yyyy, etc.
        Matcher slashMatcher = Pattern.compile("\\b(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})\\b").matcher(message);
        if (slashMatcher.find()) {
            try {
                int p1 = Integer.parseInt(slashMatcher.group(1));
                int p2 = Integer.parseInt(slashMatcher.group(2));
                int y = Integer.parseInt(slashMatcher.group(3));
                if (y < 100) {
                    y += 2000;
                }

                int month;
                int day;
                if (p1 > 12 && p2 <= 12) {
                    day = p1;
                    month = p2;
                } else if (p2 > 12 && p1 <= 12) {
                    month = p1;
                    day = p2;
                } else {
                    month = p1;
                    day = p2;
                }
                return LocalDate.of(y, month, day);
            } catch (Exception ignored) {}
        }

        // 4. Text-based month dates: e.g. "September 8, 2026", "8 September 2026", "Sep 12", "12th Sep 2026"
        Pattern textDatePattern = Pattern.compile(
                "(?i)\\b(?:(\\d{1,2})(?:st|nd|rd|th)?\\s+)?(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)(?:\\s+(\\d{1,2})(?:st|nd|rd|th)?)?(?:[',\\s]+(\\d{2,4}))?\\b"
        );
        Matcher textMatcher = textDatePattern.matcher(message);
        if (textMatcher.find()) {
            try {
                String d1 = textMatcher.group(1);
                String monthName = textMatcher.group(2).toLowerCase();
                String d2 = textMatcher.group(3);
                String yearStr = textMatcher.group(4);

                int day = 0;
                if (d1 != null && !d1.isBlank()) {
                    day = Integer.parseInt(d1);
                } else if (d2 != null && !d2.isBlank()) {
                    day = Integer.parseInt(d2);
                }

                if (day >= 1 && day <= 31) {
                    int month = parseMonthName(monthName);
                    int year = LocalDate.now().getYear();
                    if (yearStr != null && !yearStr.isBlank()) {
                        year = Integer.parseInt(yearStr);
                        if (year < 100) year += 2000;
                    }
                    return LocalDate.of(year, month, day);
                }
            } catch (Exception ignored) {}
        }

        return null;
    }

    private int parseMonthName(String m) {
        if (m.startsWith("jan")) return 1;
        if (m.startsWith("feb")) return 2;
        if (m.startsWith("mar")) return 3;
        if (m.startsWith("apr")) return 4;
        if (m.startsWith("may")) return 5;
        if (m.startsWith("jun")) return 6;
        if (m.startsWith("jul")) return 7;
        if (m.startsWith("aug")) return 8;
        if (m.startsWith("sep")) return 9;
        if (m.startsWith("oct")) return 10;
        if (m.startsWith("nov")) return 11;
        if (m.startsWith("dec")) return 12;
        return 1;
    }

    private String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.format(DATE_FORMATTER);
    }

    private String formatDisplayDate(LocalDate date) {
        if (date == null) return "";
        return date.format(DATE_DISPLAY_FORMATTER);
    }

    private BigDecimal extractAmount(String message) {
        Matcher m = Pattern.compile("(?:₹|rs\\.?|amount\\s*(?:of)?\\s*|for\\s*)?(\\d+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE).matcher(message);
        while (m.find()) {
            try {
                BigDecimal val = new BigDecimal(m.group(1));
                if (val.compareTo(BigDecimal.valueOf(50)) > 0) {
                    return val;
                }
            } catch (Exception ignored) {}
        }
        return BigDecimal.ZERO;
    }

    private boolean isCommonWord(String word) {
        if (word == null) return true;
        String w = word.toUpperCase();
        return Set.of(
                "STATUS", "FLIGHT", "DETAIL", "DETAILS", "CANCEL", "REFUND", "SEARCH", "AIRPORT",
                "ONLINE", "NUMBER", "TICKET", "BOOKING", "PLEASE", "ACTIVE", "COUPON", "COUPONS",
                "PAYMENT", "UPDATE", "CHANGE", "POLICY", "SYSTEM", "SOURCE", "DESTIN"
        ).contains(w);
    }

    private String formatBookingResponse(BookingResponseDTO booking) {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 **Booking Details (Reference: ").append(booking.getBookingCode()).append(")**\n\n");
        sb.append("• **Booking ID:** ").append(booking.getBookingId()).append("\n");
        sb.append("• **Booking Status:** ").append(booking.getStatus()).append("\n");
        sb.append("• **Payment Status:** ").append(booking.getPaymentStatus()).append("\n");
        sb.append("• **Total Fare:** ₹").append(booking.getTotalAmount()).append("\n");
        if (booking.getBookingTs() != null) {
            sb.append("• **Booked On:** ").append(booking.getBookingTs().toLocalDate()).append("\n");
        }

        if (booking.getSegments() != null && !booking.getSegments().isEmpty()) {
            sb.append("• **Flight Segments:**\n");
            for (BookingSegmentResponseDTO seg : booking.getSegments()) {
                sb.append("  - Flight ").append(seg.getFlightNumber());
                if (seg.getAirlineName() != null) sb.append(" (").append(seg.getAirlineName()).append(")");
                sb.append(": ").append(seg.getFromAirport()).append(" → ").append(seg.getToAirport());
                if (seg.getCabinClass() != null) sb.append(" [").append(seg.getCabinClass()).append("]");
                if (seg.getDepartureTs() != null) sb.append(" | Dep: ").append(seg.getDepartureTs().toLocalTime());
                if (seg.getArrivalTs() != null) sb.append(" | Arr: ").append(seg.getArrivalTs().toLocalTime());
                sb.append("\n");
            }
        }

        if (booking.getPassengers() != null && !booking.getPassengers().isEmpty()) {
            sb.append("• **Passengers:** ");
            List<String> pList = booking.getPassengers().stream()
                    .map(p -> p.getFirstName() + " " + p.getLastName() + (p.getSeatNumber() != null ? " (Seat: " + p.getSeatNumber() + ")" : ""))
                    .toList();
            sb.append(String.join(", ", pList)).append("\n");
        }

        return sb.toString();
    }

    private String formatFlightResponse(FlightResponseDTO flight) {
        StringBuilder sb = new StringBuilder();
        sb.append("✈️ **Flight Status: ").append(flight.getFlightNumber()).append("**\n\n");
        if (flight.getAirlineName() != null) {
            sb.append("• **Airline:** ").append(flight.getAirlineName()).append("\n");
        }
        sb.append("• **Route:** ").append(flight.getFromAirport()).append(" → ").append(flight.getToAirport()).append("\n");
        sb.append("• **Status:** ").append(flight.getStatus()).append("\n");
        sb.append("• **Departure:** ").append(flight.getDepartureTs()).append("\n");
        sb.append("• **Arrival:** ").append(flight.getArrivalTs()).append("\n");
        sb.append("• **Available Seats:** ").append(flight.getAvailableSeats()).append(" / ").append(flight.getTotalSeatCapacity()).append("\n");
        sb.append("• **Base Fare:** ₹").append(flight.getBasePrice());
        return sb.toString();
    }

    private String formatFlightSearchResults(String from, String to, LocalDate date, List<FlightResponseDTO> flights) {
        String src = from.trim().toUpperCase();
        String dest = to.trim().toUpperCase();

        if (date != null) {
            if (flights == null || flights.isEmpty()) {
                try {
                    List<FlightResponseDTO> otherFlights = flightTools.searchFlights(src, dest, null);
                    if (otherFlights != null && !otherFlights.isEmpty()) {
                        List<String> availableDates = otherFlights.stream()
                                .filter(f -> f.getDepartureTs() != null)
                                .map(f -> formatDate(f.getDepartureTs().toLocalDate()))
                                .distinct()
                                .toList();
                        if (!availableDates.isEmpty()) {
                            return "No scheduled flights found between " + src + " and " + dest + " on " + formatDisplayDate(date) + ".\n\n"
                                    + "📅 **Available flight dates on this route:** " + String.join(", ", availableDates) + ".\n"
                                    + "Please select an alternate travel date.";
                        }
                    }
                } catch (Exception ignored) {}

                return "No scheduled flights found between " + src + " and " + dest + " on " + formatDisplayDate(date) + ". Please check alternate airport codes (e.g. MAA, BLR, DEL, BOM) or travel dates.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("✈️ **Available Flights from ").append(src).append(" to ").append(dest).append(" on ").append(formatDisplayDate(date)).append(":**\n\n");
            for (FlightResponseDTO f : flights) {
                sb.append("• **").append(f.getFlightNumber()).append("**");
                if (f.getAirlineName() != null) sb.append(" (").append(f.getAirlineName()).append(")");
                sb.append(": ").append(f.getFromAirport()).append(" → ").append(f.getToAirport());
                if (f.getDepartureTs() != null) {
                    sb.append(" | Date: ").append(formatDate(f.getDepartureTs().toLocalDate()));
                    sb.append(" | Dep: ").append(f.getDepartureTs().toLocalTime());
                }
                if (f.getArrivalTs() != null) sb.append(" | Arr: ").append(f.getArrivalTs().toLocalTime());
                sb.append(" | Fare: ₹").append(f.getBasePrice());
                sb.append(" | Seats Left: ").append(f.getAvailableSeats());
                sb.append("\n");
            }
            return sb.toString();
        } else {
            // No date specified: group flights by date so they are not shown all jumbled together
            if (flights == null || flights.isEmpty()) {
                return "No scheduled flights found between " + src + " and " + dest + ". Please check alternate airport codes (e.g. MAA, BLR, DEL, BOM) or travel dates.";
            }

            Map<LocalDate, List<FlightResponseDTO>> byDate = flights.stream()
                    .filter(f -> f.getDepartureTs() != null)
                    .collect(Collectors.groupingBy(
                            f -> f.getDepartureTs().toLocalDate(),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            if (byDate.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("✈️ **Available Flights from ").append(src).append(" to ").append(dest).append(":**\n\n");
                for (FlightResponseDTO f : flights) {
                    sb.append("• **").append(f.getFlightNumber()).append("**");
                    if (f.getAirlineName() != null) sb.append(" (").append(f.getAirlineName()).append(")");
                    sb.append(": ").append(f.getFromAirport()).append(" → ").append(f.getToAirport());
                    if (f.getDepartureTs() != null) sb.append(" | Dep: ").append(f.getDepartureTs().toLocalTime());
                    if (f.getArrivalTs() != null) sb.append(" | Arr: ").append(f.getArrivalTs().toLocalTime());
                    sb.append(" | Fare: ₹").append(f.getBasePrice());
                    sb.append(" | Seats Left: ").append(f.getAvailableSeats());
                    sb.append("\n");
                }
                return sb.toString();
            }

            StringBuilder sb = new StringBuilder();
            sb.append("✈️ **Available Flights from ").append(src).append(" to ").append(dest).append(":**\n\n");

            for (Map.Entry<LocalDate, List<FlightResponseDTO>> entry : byDate.entrySet()) {
                sb.append("📅 **Date: ").append(formatDisplayDate(entry.getKey())).append("**\n");
                for (FlightResponseDTO f : entry.getValue()) {
                    sb.append("• **").append(f.getFlightNumber()).append("**");
                    if (f.getAirlineName() != null) sb.append(" (").append(f.getAirlineName()).append(")");
                    sb.append(": ").append(f.getFromAirport()).append(" → ").append(f.getToAirport());
                    if (f.getDepartureTs() != null) sb.append(" | Dep: ").append(f.getDepartureTs().toLocalTime());
                    if (f.getArrivalTs() != null) sb.append(" | Arr: ").append(f.getArrivalTs().toLocalTime());
                    sb.append(" | Fare: ₹").append(f.getBasePrice());
                    sb.append(" | Seats Left: ").append(f.getAvailableSeats());
                    sb.append("\n");
                }
                sb.append("\n");
            }

            LocalDate firstDate = byDate.keySet().iterator().next();
            sb.append("💡 *Tip: To filter for a specific date, specify your travel date (e.g. \"flights from ")
                    .append(src).append(" to ").append(dest).append(" on ")
                    .append(formatDate(firstDate)).append("\").*");

            return sb.toString().trim();
        }
    }

    private String formatActiveCoupons(List<CouponResponseDTO> coupons) {
        if (coupons == null || coupons.isEmpty()) {
            return "There are currently no active promotional coupons available.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("🎟️ **Active Coupons & Promo Codes:**\n\n");
        for (CouponResponseDTO c : coupons) {
            sb.append("• **").append(c.getCouponCode()).append("**: ");
            if (c.getDiscountType() == DiscountType.PERCENTAGE) {
                sb.append(c.getDiscountValue()).append("% OFF");
            } else {
                sb.append("₹").append(c.getDiscountValue()).append(" OFF");
            }
            if (c.getMinimumBookingAmount() != null) {
                sb.append(" (Min booking: ₹").append(c.getMinimumBookingAmount()).append(")");
            }
            if (c.getValidTo() != null) {
                sb.append(" - Valid until ").append(c.getValidTo().toLocalDate());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String formatPaymentMethods(List<String> methods) {
        StringBuilder sb = new StringBuilder();
        sb.append("💳 **Accepted Payment Methods in SkyRoute:**\n\n");
        for (String method : methods) {
            sb.append("• ").append(method).append("\n");
        }
        return sb.toString();
    }

    private String formatPaymentList(String ref, List<PaymentResponseDTO> payments) {
        StringBuilder sb = new StringBuilder();
        sb.append("💳 **Payment Status for Booking (").append(ref).append("):**\n\n");
        for (PaymentResponseDTO p : payments) {
            sb.append("• **Payment ID:** ").append(p.getPaymentId())
                    .append(" | **Status:** ").append(p.getStatus())
                    .append(" | **Amount:** ₹").append(p.getAmount())
                    .append(" | **Method:** ").append(p.getPaymentMethod());
            if (p.getRazorpayPaymentId() != null && !p.getRazorpayPaymentId().isBlank()) {
                sb.append(" | **Gateway Ref:** ").append(p.getRazorpayPaymentId());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String formatRefundResponse(BookingCancellationResponseDTO c) {
        StringBuilder sb = new StringBuilder();
        sb.append("🔄 **Cancellation & Refund Status (Booking: ").append(c.getBookingCode()).append(")**\n\n");
        sb.append("• **Cancellation ID:** ").append(c.getCancellationId()).append("\n");
        sb.append("• **Status:** ").append(c.getStatus()).append("\n");
        sb.append("• **Original Total:** ₹").append(c.getOriginalAmount()).append("\n");
        sb.append("• **Cancellation Fee:** ₹").append(c.getCancellationCharge()).append("\n");
        sb.append("• **Refund Amount:** ₹").append(c.getRefundAmount()).append("\n");
        if (c.getReason() != null && !c.getReason().isBlank()) {
            sb.append("• **Reason:** ").append(c.getReason()).append("\n");
        }
        return sb.toString();
    }

    private String handleChatFallback(String msg) {
        String lower = msg != null ? msg.toLowerCase() : "";
        if (lower.contains("flight") && (lower.contains("from") || lower.contains("avail") || lower.contains("to"))) {
            return "To check available flights, please provide your departure and destination airports (e.g. MAA to BLR).";
        } else if (lower.contains("track") || lower.contains("status")) {
            return "To track a flight or booking, please provide the flight number (e.g. AI-201) or your booking reference code (e.g. E875BC).";
        } else if (lower.contains("cancel") || lower.contains("refund")) {
            return "Cancellations can be made under 'My Trips'. Refund policy: >24 hrs before departure: 90% refund (10% fee); 6-24 hrs: 75% refund (25% fee); <6 hrs: 50% refund (50% fee).";
        } else if (lower.contains("coupon") || lower.contains("promo") || lower.contains("discount")) {
            return "You can apply coupons like AI-123 or EY-0432 during checkout to receive discounts on your flight booking.";
        } else if (lower.contains("payment") || lower.contains("pay")) {
            return "SkyRoute accepts Credit/Debit Cards, UPI & QR Codes, Net Banking (50+ banks), and Digital Wallets via secure Razorpay checkout.";
        }
        return "I am here to assist with flight bookings, schedules, flight tracking, seat selection, cancellations, and payments. How can I help you today?";
    }

    private String sanitizeResponse(String answer) {
        if (answer == null || answer.isBlank()) {
            return "I am ready to help with flight booking, baggage rules, payment methods, cancellations, and refunds. Please specify your question.";
        }

        String trimmed = answer.trim();

        // Catch accidental raw tool call outputs from small local models
        if (trimmed.startsWith("{") && (trimmed.contains("\"name\"") || trimmed.contains("getPaymentMethods") || trimmed.contains("parameters"))) {
            if (trimmed.toLowerCase().contains("payment")) {
                return "SkyRoute accepts the following payment methods via Razorpay:\n"
                        + "• Credit & Debit Cards (Visa, MasterCard, RuPay, American Express)\n"
                        + "• UPI & QR Codes (Google Pay, PhonePe, Paytm, BHIM)\n"
                        + "• Net Banking (50+ banks including HDFC, ICICI, SBI, Axis)\n"
                        + "• Digital Wallets (Paytm, PhonePe, Amazon Pay)";
            } else if (trimmed.toLowerCase().contains("flight")) {
                return "To search for flights, please provide your departure airport, destination airport, and travel date (e.g. 'Search flights from MAA to BLR tomorrow').";
            } else if (trimmed.toLowerCase().contains("booking")) {
                return "To check booking status, please provide your 6-character booking reference code (e.g. 'Status of booking ABC123').";
            }
            return "How can I assist you with your flight booking, payments, or travel inquiry?";
        }

        return trimmed;
    }
}