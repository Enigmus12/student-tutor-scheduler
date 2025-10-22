package edu.eci.arsw.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
/** Vista de una reserva */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationView {
    private String id;
    private String tutorId;
    private String studentId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDate date;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalTime start;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalTime end;

    private String status; 

    // Enriquecido desde /public/profile 
    private String studentName;
    private String studentAvatar;
    private String tutorName;
    private String tutorAvatar;
}
