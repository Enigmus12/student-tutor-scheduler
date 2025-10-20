package edu.eci.arsw.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
/** Celda del horario de un tutor */
@Data @AllArgsConstructor @NoArgsConstructor
public class ScheduleCell {
    private String date; // YYYY-MM-DD
    private String hour; // HH:mm
    private String status; // DISPONIBLE | ACTIVA | ACEPTADO | CANCELADO | null (no disponible)
    private String reservationId;
    private String studentId;
}
