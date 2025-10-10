package co.eci.uplearn.reservation.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateReservationRequest(
    @NotBlank String tutorId,
    @NotNull LocalDate day,
    @NotNull LocalTime start,
    @NotNull LocalTime end
) {}
