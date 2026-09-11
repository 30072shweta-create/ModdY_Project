package com.example.flight.tools;

import java.time.LocalDate;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import com.example.flight.dto.FlightResponseDTO;
import com.example.flight.dto.FlightSearchRequestDTO;
import com.example.flight.service.FlightService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FlightTools {

    private final FlightService flightService;

    @Tool(description = """
            Search for available flights between a source and destination airport or city (e.g. MAA to BLR, or Chennai to Bangalore).
            Use this tool when the user asks about available flights from source to destination,
            flight availability, schedules, prices, or airlines between two locations.
            Source and destination can be airport codes (such as MAA, BLR, DEL, BOM) or city names (such as Chennai, Bangalore, Delhi, Mumbai).
            Date is optional. When a travel date is provided by the user, specify it to filter flights strictly for that date.
            """)
    public List<FlightResponseDTO> searchFlights(
            String source,
            String destination,
            LocalDate date) {
        System.out.println("FLIGHT SEARCH TOOL CALLED: source=" + source + ", dest=" + destination + ", date=" + date);

        FlightSearchRequestDTO request = new FlightSearchRequestDTO();

        if (source != null) {
            request.setSource(source.trim().replaceAll("[\"']", ""));
        }
        if (destination != null) {
            request.setDestination(destination.trim().replaceAll("[\"']", ""));
        }
        request.setDate(date);
        request.setPage(0);
        request.setSize(25);

        return flightService
                .searchFlights(request)
                .getContent();
    }

    @Tool(description = """
            Track flight live status, schedule, departure time, arrival time, and aircraft details using a flight number (e.g. AI101, 6E202, UK808).
            Use this tool when the user asks to track a flight, check flight status, check if a flight is on time, scheduled, or delayed,
            or asks for details of a specific flight number.
            Flight number is required.
            """)
    public FlightResponseDTO trackFlight(String flightNumber) {
        System.out.println("FLIGHT TRACKING TOOL CALLED FOR: " + flightNumber);
        if (flightNumber == null || flightNumber.isBlank()) {
            throw new IllegalArgumentException("Flight number is required for tracking.");
        }
        String cleanNumber = flightNumber.trim().replaceAll("[\"']", "");
        return flightService.trackFlight(cleanNumber);
    }

    @Tool(description = """
            Check seat availability and remaining seat count on a specific flight by flight number (e.g. AI101, 6E202).
            Use this tool when the user asks how many seats are available or left on a specific flight.
            """)
    public String checkSeatAvailability(String flightNumber) {
        System.out.println("SEAT AVAILABILITY TOOL CALLED FOR: " + flightNumber);
        if (flightNumber == null || flightNumber.isBlank()) {
            return "Please provide a valid flight number to check seat availability.";
        }
        try {
            FlightResponseDTO flight = flightService.trackFlight(flightNumber.trim().replaceAll("[\"']", ""));
            return "Flight " + flight.getFlightNumber() + " (" + flight.getFromAirport() + " → " + flight.getToAirport() + ") has "
                    + flight.getAvailableSeats() + " seats available out of " + flight.getTotalSeatCapacity() + " total capacity. Base price: ₹"
                    + flight.getBasePrice();
        } catch (Exception e) {
            return "Could not find flight with number: " + flightNumber;
        }
    }
}