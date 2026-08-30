package com.example.flight.tools;

import com.example.flight.dto.FlightResponseDTO;
import com.example.flight.dto.FlightSearchRequestDTO;
import com.example.flight.service.FlightService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class FlightTools {

    private final FlightService flightService;

    public FlightTools(FlightService flightService) {
        this.flightService = flightService;
    }

    @Tool(description = """
            Search for live flight information from the database.

            Use this tool when the user asks about:
            - available flights
            - flights between two locations
            - flight schedules
            - flight prices
            - live flight availability

            Source and destination should match the airport codes
            used by the Flight Management System database.

            If the user provides a travel date, pass it to the tool.
            """)
    public List<FlightResponseDTO> searchFlights(
            String source,
            String destination,
            LocalDate date) {

        FlightSearchRequestDTO request =
                new FlightSearchRequestDTO();

        request.setSource(source);
        request.setDestination(destination);
        request.setDate(date);

        request.setPage(0);
        request.setSize(10);

        return flightService
                .searchFlights(request)
                .getContent();
    }
}