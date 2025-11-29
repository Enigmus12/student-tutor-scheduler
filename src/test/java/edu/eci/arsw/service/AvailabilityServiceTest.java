package edu.eci.arsw.service;

import edu.eci.arsw.domain.AvailabilitySlot;
import edu.eci.arsw.dto.BulkAvailabilityRequest;
import edu.eci.arsw.repository.AvailabilitySlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.params.provider.Arguments;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceExtraTest {

    @Mock
    private AvailabilitySlotRepository repo;

    private AvailabilityService service;

    @BeforeEach
    void setUp() {
        service = new AvailabilityService(repo);
    }


    static Stream<Arguments> invalidBulkRequests() {
        BulkAvailabilityRequest fromAfterTo = new BulkAvailabilityRequest();
        fromAfterTo.setFromDate(LocalDate.of(2025, 1, 10));
        fromAfterTo.setToDate(LocalDate.of(2025, 1, 9));
        fromAfterTo.setFromHour("08:00");
        fromAfterTo.setToHour("10:00");

        BulkAvailabilityRequest fromHourNotOnTheHour = new BulkAvailabilityRequest();
        fromHourNotOnTheHour.setFromDate(LocalDate.of(2025, 1, 1));
        fromHourNotOnTheHour.setToDate(LocalDate.of(2025, 1, 1));
        fromHourNotOnTheHour.setFromHour("08:30");
        fromHourNotOnTheHour.setToHour("10:00");

        BulkAvailabilityRequest toNotAfterFrom = new BulkAvailabilityRequest();
        toNotAfterFrom.setFromDate(LocalDate.of(2025, 1, 1));
        toNotAfterFrom.setToDate(LocalDate.of(2025, 1, 1));
        toNotAfterFrom.setFromHour("10:00");
        toNotAfterFrom.setToHour("10:00");

        return Stream.of(
                Arguments.of("fromDate > toDate", fromAfterTo),
                Arguments.of("fromHour not on the hour", fromHourNotOnTheHour),
                Arguments.of("toHour not after fromHour", toNotAfterFrom)
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("invalidBulkRequests")
    void bulkCreateShouldRejectInvalidRequests(String name, BulkAvailabilityRequest req) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.bulkCreate("t1", req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }


    @Test
    void bulkCreateShouldHandleDuplicateKeyGracefully() {
        BulkAvailabilityRequest req = new BulkAvailabilityRequest();
        req.setFromDate(LocalDate.of(2025, 1, 1)); 
        req.setToDate(LocalDate.of(2025, 1, 2));  
        req.setFromHour("08:00");
        req.setToHour("10:00");
        req.setDaysOfWeek(List.of(DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY));

        when(repo.save(any(AvailabilitySlot.class))).thenAnswer(invocation -> {
            AvailabilitySlot s = invocation.getArgument(0);
            if (s.getDate().equals(LocalDate.of(2025, 1, 2))) {
                throw new DuplicateKeyException("dup");
            }
            return s;
        });

        List<AvailabilitySlot> created = service.bulkCreate("t1", req);

        assertFalse(created.isEmpty()); 
    }

    @Test
    void deleteOwnSlotShouldValidateOwnershipAndActiveReservation() {
        AvailabilitySlot slot = AvailabilitySlot.builder()
                .id("slot1").tutorId("t1").build();
        when(repo.findById("slot1")).thenReturn(Optional.of(slot));

        ResponseStatusException conflict = assertThrows(ResponseStatusException.class,
                () -> service.deleteOwnSlot("t1", "slot1", true));
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());

        ResponseStatusException forbidden = assertThrows(ResponseStatusException.class,
                () -> service.deleteOwnSlot("other", "slot1", false));
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());

        service.deleteOwnSlot("t1", "slot1", false);
        verify(repo).deleteById("slot1");
    }

    @Test
    void deleteOwnSlotShouldFailWhenSlotDoesNotExist() {
        when(repo.findById("missing")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.deleteOwnSlot("t1", "missing", false));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void replaceDayShouldValidateHourPrecision() {
        LocalDate date = LocalDate.of(2025, 1, 1);
        List<LocalTime> invalidHours = List.of(LocalTime.of(10, 30));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.replaceDay("t1", date, invalidHours, Collections.emptySet()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void replaceDayShouldDeleteOnlyInactiveSlotsAndInsertNewOnes() {
        LocalDate date = LocalDate.of(2025, 1, 1);

        LocalTime h1 = LocalTime.of(8, 0);
        LocalTime h2 = LocalTime.of(9, 0);
        AvailabilitySlot existing1 = AvailabilitySlot.builder()
                .id("s1").tutorId("t1").date(date).start(h1).end(h1.plusHours(1)).build();
        AvailabilitySlot existing2 = AvailabilitySlot.builder()
                .id("s2").tutorId("t1").date(date).start(h2).end(h2.plusHours(1)).build();

        when(repo.findByTutorIdAndDate("t1", date)).thenReturn(List.of(existing1, existing2));

        LocalTime newHour = LocalTime.of(10, 0);
        Set<LocalTime> active = Set.of(h2);

        service.replaceDay("t1", date, List.of(newHour), active);

        verify(repo).deleteById("s1");
        verify(repo, never()).deleteById("s2");
        verify(repo).save(argThat(slot ->
                "t1".equals(slot.getTutorId())
                        && date.equals(slot.getDate())
                        && newHour.equals(slot.getStart())
                        && newHour.plusHours(1).equals(slot.getEnd())
        ));
    }

    @Test
    void addAvailabilityShouldRejectNonExactHours() {
        LocalDate date = LocalDate.of(2025, 1, 1);
        List<LocalTime> hours = List.of(LocalTime.of(10, 30));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.addAvailability("t1", date, hours));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void addAvailabilityShouldSkipExistingAndCountAddedAndHandleDuplicateKey() {
        LocalDate date = LocalDate.of(2025, 1, 1);
        LocalTime existingHour = LocalTime.of(8, 0);
        LocalTime newHour1 = LocalTime.of(9, 0);
        LocalTime newHour2 = LocalTime.of(10, 0);

        AvailabilitySlot existingSlot = AvailabilitySlot.builder()
                .tutorId("t1").date(date).start(existingHour).end(existingHour.plusHours(1)).build();

        when(repo.findByTutorIdAndDate("t1", date)).thenReturn(List.of(existingSlot));

        when(repo.save(any(AvailabilitySlot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0))
                .thenThrow(new DuplicateKeyException("dup"));

        int added = service.addAvailability("t1", date,
                List.of(existingHour, newHour1, newHour2));

        assertEquals(1, added); 
        verify(repo, times(2)).save(any(AvailabilitySlot.class));
    }

    @Test
    void mySlotsAndSlotsForDayShouldDelegateToRepository() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 7);

        when(repo.findByTutorIdAndDateGreaterThanEqualAndDateLessThanEqual("t1", from, to))
                .thenReturn(Collections.emptyList());
        when(repo.findByTutorIdAndDate("t1", from)).thenReturn(Collections.emptyList());

        assertNotNull(service.mySlots("t1", from, to));
        assertNotNull(service.slotsForDay("t1", from));
    }
}
 