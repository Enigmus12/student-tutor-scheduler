package edu.eci.arsw.infra;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class MongoConfigTest {

    private final MongoConfig config = new MongoConfig();

    @Test
    void localTimeConvertersShouldRoundTrip() {
        MongoConfig.LocalTimeToStringConverter to =
                new MongoConfig.LocalTimeToStringConverter();
        MongoConfig.StringToLocalTimeConverter from =
                new MongoConfig.StringToLocalTimeConverter();

        LocalTime t = LocalTime.of(9, 15);
        String serialized = to.convert(t);
        assertEquals("09:15", serialized);

        assertEquals(t, from.convert(serialized));
        // también debe aceptar HH:mm:ss (recorta a HH:mm)
        assertEquals(LocalTime.of(9, 15), from.convert("09:15:59"));
    }

    @Test
    void localDateConvertersShouldRoundTrip() {
        MongoConfig.LocalDateToStringConverter to =
                new MongoConfig.LocalDateToStringConverter();
        MongoConfig.StringToLocalDateConverter from =
                new MongoConfig.StringToLocalDateConverter();

        LocalDate d = LocalDate.of(2025, 1, 2);
        String s = to.convert(d);
        assertEquals("2025-01-02", s);
        assertEquals(d, from.convert(s));
    }

    @Test
    void customConversionsBeanShouldBeCreated() {
        MongoCustomConversions conversions = config.customConversions();
        assertNotNull(conversions);
    }

    @Test
    void stringToLocalTimeConverterShouldHandleNullAndSeconds() {
        MongoConfig.StringToLocalTimeConverter conv =
                new MongoConfig.StringToLocalTimeConverter();

        assertEquals(LocalTime.of(0, 0), conv.convert(null));
        assertEquals(LocalTime.of(10, 15), conv.convert("10:15:30"));
    }

    @Test
    void dateConvertersShouldRoundTrip() {
        MongoConfig.LocalDateToStringConverter toString =
                new MongoConfig.LocalDateToStringConverter();
        MongoConfig.StringToLocalDateConverter toDate =
                new MongoConfig.StringToLocalDateConverter();

        LocalDate original = LocalDate.of(2025, 5, 20);
        String encoded = toString.convert(original);
        LocalDate decoded = toDate.convert(encoded);

        assertEquals(original, decoded);
    }
}
