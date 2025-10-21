// src/main/java/edu/eci/arsw/infra/MongoConfig.java
package edu.eci.arsw.infra;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.core.convert.converter.Converter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/** Configuración de conversiones personalizadas para MongoDB */
@Configuration
public class MongoConfig {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    /** Registrar conversiones personalizadas */
    @Bean
    public MongoCustomConversions customConversions() {
        return new MongoCustomConversions(Arrays.asList(
                new LocalTimeToStringConverter(),
                new StringToLocalTimeConverter(),
                new LocalDateToStringConverter(),
                new StringToLocalDateConverter()));
    }

    /** Convierte LocalTime a "HH:mm" para MongoDB */
    static class LocalTimeToStringConverter implements Converter<LocalTime, String> {
        @Override
        public String convert(LocalTime source) {
            return source.format(TIME_FMT);
        }
    }

    /** Convierte String ("HH:mm" o "HH:mm:ss") a LocalTime */
    static class StringToLocalTimeConverter implements Converter<String, LocalTime> {
        @Override
        public LocalTime convert(String source) {
            String s = (source == null) ? "00:00" : source.trim();
            if (s.length() >= 5)
                s = s.substring(0, 5); // recorta a HH:mm si viene HH:mm:ss
            return LocalTime.parse(s, TIME_FMT);
        }
    }

    /** Convierte LocalDate a "yyyy-MM-dd" para MongoDB */
    static class LocalDateToStringConverter implements Converter<LocalDate, String> {
        @Override
        public String convert(LocalDate source) {
            return source.format(DATE_FMT);
        }
    }

    /** Convierte String ("yyyy-MM-dd") a LocalDate */
    static class StringToLocalDateConverter implements Converter<String, LocalDate> {
        @Override
        public LocalDate convert(String source) {
            return LocalDate.parse(source, DATE_FMT);
        }
    }
}
