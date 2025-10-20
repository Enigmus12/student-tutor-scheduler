package edu.eci.arsw.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
/** Solicitud para actualizar la disponibilidad de un día */
@Data
public class DayAvailabilityUpdateRequest {
    @NotEmpty
    private List<String> hours; // ["08:00","09:00"]
}
