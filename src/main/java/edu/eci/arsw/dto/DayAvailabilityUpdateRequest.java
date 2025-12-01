package edu.eci.arsw.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** Solicitud para actualizar la disponibilidad de un día */
@Data
public class DayAvailabilityUpdateRequest {
    @NotNull(message = "hours no puede ser null")
    private List<String> hours = new ArrayList<>(); 
}
