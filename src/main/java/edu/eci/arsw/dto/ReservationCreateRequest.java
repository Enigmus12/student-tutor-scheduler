package edu.eci.arsw.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/** Solicitud para crear una reserva */
@Data
public class ReservationCreateRequest {
    @NotBlank private String tutorId;
    @NotNull private LocalDate date;
    @NotBlank private String hour; 
}
