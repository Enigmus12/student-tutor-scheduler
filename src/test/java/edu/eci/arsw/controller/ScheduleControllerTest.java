package edu.eci.arsw.controller;

import edu.eci.arsw.dto.ScheduleCell;
import edu.eci.arsw.security.AuthorizationService;
import edu.eci.arsw.service.ScheduleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleControllerTest {

    @Mock
    private ScheduleService scheduleService;

    @Mock
    private AuthorizationService authz;

    @InjectMocks
    private ScheduleController controller;

    private static final String TOKEN = "Bearer token";

    @Test
    void week_shouldReturnScheduleForTutor() {
        String tutorId = "t1";
        LocalDate weekStart = LocalDate.of(2025, 1, 6);

        List<ScheduleCell> cells = List.of(
                new ScheduleCell("2025-01-06", "10:00", "DISPONIBLE", null, null)
        );
        when(scheduleService.weekForTutor(tutorId, weekStart)).thenReturn(cells);

        ResponseEntity<List<ScheduleCell>> response =
                controller.week(TOKEN, tutorId, weekStart);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(cells, response.getBody());
        verify(authz).requireRole(TOKEN, "STUDENT", "TUTOR");
    }

    @Test
    void week_shouldReturnEmptyListWhenNoSlots() {
        String tutorId = "t1";
        LocalDate weekStart = LocalDate.of(2025, 1, 6);

        when(scheduleService.weekForTutor(tutorId, weekStart))
                .thenReturn(Collections.emptyList());

        ResponseEntity<List<ScheduleCell>> response =
                controller.week(TOKEN, tutorId, weekStart);

        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void week_shouldFailWhenAuthServiceThrows() {
        String tutorId = "t1";
        LocalDate weekStart = LocalDate.of(2025, 1, 6);

        doThrow(new RuntimeException("Forbidden"))
                .when(authz).requireRole(TOKEN, "STUDENT", "TUTOR");

        assertThrows(RuntimeException.class,
                () -> controller.week(TOKEN, tutorId, weekStart));
        verify(scheduleService, never())
                .weekForTutor(anyString(), any());
    }

    @Test
    void week_shouldPropagateServiceError() {
        String tutorId = "t1";
        LocalDate weekStart = LocalDate.of(2025, 1, 6);

        doThrow(new RuntimeException("DB error"))
                .when(scheduleService).weekForTutor(tutorId, weekStart);

        assertThrows(RuntimeException.class,
                () -> controller.week(TOKEN, tutorId, weekStart));
    }
}
