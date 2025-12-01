package edu.eci.arsw.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Celda del horario de un tutor */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleCell {
    private String date; // YYYY-MM-DD
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private String hour; // HH:mm
    private String status; // DISPONIBLE | ACTIVA | ACEPTADO | CANCELADO | null (no disponible)
    private String reservationId;
    private String studentId;
}
