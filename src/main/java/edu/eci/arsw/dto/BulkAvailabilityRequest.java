package edu.eci.arsw.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
/** Solicitud para actualizar la disponibilidad en bloque */
@Data
public class BulkAvailabilityRequest {
    @NotNull private LocalDate fromDate;
    @NotNull private LocalDate toDate;
    @NotNull private String fromHour; // "08:00"
    @NotNull private String toHour;   // "17:00" (no inclusive)
    private List<DayOfWeek> daysOfWeek;
    private String timezone;
}
