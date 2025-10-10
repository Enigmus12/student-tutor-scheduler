package co.eci.uplearn.reservation.web.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReservationResponse(
    String id,
    String studentId,
    String tutorId,
    LocalDate day,
    LocalTime start,
    LocalTime end,
    String status
) {}
