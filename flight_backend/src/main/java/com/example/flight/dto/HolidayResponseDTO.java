package com.example.flight.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HolidayResponseDTO {

    private Long holidayId;
    private String name;
    private LocalDate holidayDate;
    private Boolean active;
}
