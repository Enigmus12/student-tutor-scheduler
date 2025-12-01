package edu.eci.arsw.controller;

import edu.eci.arsw.dto.ReservationView;
import edu.eci.arsw.service.ReservationQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationQueryControllerTest {

    @Mock
    private ReservationQueryService service;

    @InjectMocks
    private ReservationQueryController controller;

    // ========== byTutor() ==========

    @Test
    void byTutor_shouldReturnReservationsForTutor() {
        String tutorId = "t1";
        List<ReservationView> views = List.of(
                ReservationView.builder().id("r1").tutorId(tutorId).build());
        when(service.listByTutor(tutorId)).thenReturn(views);

        List<ReservationView> result = controller.byTutor(tutorId);

        assertEquals(1, result.size());
        assertEquals(tutorId, result.get(0).getTutorId());
    }

    @Test
    void byTutor_shouldReturnEmptyListWhenNoReservations() {
        String tutorId = "t1";
        when(service.listByTutor(tutorId)).thenReturn(Collections.emptyList());

        List<ReservationView> result = controller.byTutor(tutorId);

        assertTrue(result.isEmpty());
    }

    @Test
    void byTutor_shouldPropagateServiceError() {
        String tutorId = "t1";
        when(service.listByTutor(tutorId))
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class,
                () -> controller.byTutor(tutorId));
    }

    @Test
    void byTutor_shouldHandleNullTutorId() {
        when(service.listByTutor(null)).thenReturn(Collections.emptyList());

        List<ReservationView> result = controller.byTutor(null);

        assertNotNull(result);
    }

    // ========== byStudent() ==========

    @Test
    void byStudent_shouldReturnReservationsForStudent() {
        String studentId = "s1";
        List<ReservationView> views = List.of(
                ReservationView.builder().id("r1").studentId(studentId).build());
        when(service.listByStudent(studentId)).thenReturn(views);

        List<ReservationView> result = controller.byStudent(studentId);

        assertEquals(1, result.size());
        assertEquals(studentId, result.get(0).getStudentId());
    }

    @Test
    void byStudent_shouldReturnEmptyListWhenNoReservations() {
        String studentId = "s1";
        when(service.listByStudent(studentId)).thenReturn(Collections.emptyList());

        List<ReservationView> result = controller.byStudent(studentId);

        assertTrue(result.isEmpty());
    }

    @Test
    void byStudent_shouldPropagateServiceError() {
        String studentId = "s1";
        when(service.listByStudent(studentId))
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class,
                () -> controller.byStudent(studentId));
    }

    @Test
    void byStudent_shouldHandleNullStudentId() {
        when(service.listByStudent(null)).thenReturn(Collections.emptyList());

        List<ReservationView> result = controller.byStudent(null);

        assertNotNull(result);
    }
}
