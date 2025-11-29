package edu.eci.arsw.infra;

import java.time.*;

/** Utilidades relacionadas con el tiempo */
public class TimeUtils {
    private TimeUtils() {
        // prevent instantiation
    }

    /**
     * Verifica si un tiempo está en la hora exacta
     * 
     * @param t Tiempo a verificar
     * @return true si está en la hora exacta, false en caso contrario
     */
    public static boolean isOnTheHour(LocalTime t) {
        return t.getMinute() == 0 && t.getSecond() == 0 && t.getNano() == 0;
    }

    /**
     * Parsea una cadena en formato "HH" o "HH:mm" a LocalTime
     * 
     * @param hhmm Cadena con la hora
     * @return Objeto LocalTime correspondiente
     */
    public static LocalTime parseHour(String hhmm) {
        return LocalTime.parse(hhmm + (hhmm.length() == 5 ? ":00" : ""));
    }

    /**
     * Verifica si un tiempo ha pasado
     * 
     * @param date Fecha a verificar
     * @param hour Hora a verificar2
     */
    public static boolean isPast(LocalDate date, LocalTime hour, ZoneId zone) {
        ZonedDateTime z = ZonedDateTime.of(date, hour, zone);
        return z.isBefore(ZonedDateTime.now(zone));
    }
}
