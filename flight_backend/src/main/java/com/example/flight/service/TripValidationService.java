package com.example.flight.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.flight.entity.Flight;

@Service
public class TripValidationService {

    // Validate all selected flights
    public void validateTrip(List<Flight> flights) {

        if (flights == null || flights.isEmpty()) {
            throw new RuntimeException(
                    "At least one flight is required"
            );
        }

        // Check connection between segments
        for (int i = 1; i < flights.size(); i++) {

            Flight previousFlight = flights.get(i - 1);
            Flight currentFlight = flights.get(i);

            String previousDestination =
                    previousFlight.getToAirport()
                            .getAirportCode();

            String currentSource =
                    currentFlight.getFromAirport()
                            .getAirportCode();

            // Airport connection validation
            if (!previousDestination
                    .equalsIgnoreCase(currentSource)) {

                throw new RuntimeException(
                        "Invalid trip: Flight "
                                + (i + 1)
                                + " must start from "
                                + previousDestination
                );
            }

            // Time validation
            if (currentFlight.getDepartureTs()
                    .isBefore(
                            previousFlight.getArrivalTs()
                    )) {

                throw new RuntimeException(
                        "Invalid trip: Flight "
                                + (i + 1)
                                + " departs before "
                                + "the previous flight arrives"
                );
            }
        }
    }


    // Determine booking type
    public String determineTripType(
            List<Flight> flights) {

        if (flights.size() == 1) {
            return "ONE_WAY";
        }

        if (flights.size() == 2) {

            String firstSource =
                    flights.get(0)
                            .getFromAirport()
                            .getAirportCode();

            String lastDestination =
                    flights.get(1)
                            .getToAirport()
                            .getAirportCode();

            // Example:
            // Bengaluru → Delhi
            // Delhi → Bengaluru

            if (firstSource.equalsIgnoreCase(
                    lastDestination)) {

                return "ROUND_TRIP";
            }
        }

        return "MULTI_CITY";
    }
}