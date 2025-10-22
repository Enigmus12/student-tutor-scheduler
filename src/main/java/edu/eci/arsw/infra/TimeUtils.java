package edu.eci.arsw.infra;

import java.time.*;

/** Utilidades relacionadas con el tiempo */
public class TimeUtils {
    private TimeUtils() {
        // prevent instantiation
    }

    /** Verifica si un tiempo está en la hora exacta */
    public static boolean isOnTheHour(LocalTime t) {
        return t.getMinute() == 0 && t.getSecond() == 0 && t.getNano() == 0;
    }

    /** Parsea una cadena en formato "HH" o "HH:mm" a LocalTime */
    public static LocalTime parseHour(String hhmm) {
        return LocalTime.parse(hhmm + (hhmm.length() == 5 ? ":00" : ""));
    }

    /** Verifica si un tiempo ha pasado */
    public static boolean isPast(LocalDate date, LocalTime hour, ZoneId zone) {
        ZonedDateTime z = ZonedDateTime.of(date, hour, zone);
        return z.isBefore(ZonedDateTime.now(zone));
    }
}
