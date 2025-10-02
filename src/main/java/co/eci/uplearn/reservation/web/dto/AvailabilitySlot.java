package co.eci.uplearn.reservation.web.dto;

import java.time.LocalTime;

public record AvailabilitySlot(
    LocalTime start,
    LocalTime end,
    boolean available
) {}
