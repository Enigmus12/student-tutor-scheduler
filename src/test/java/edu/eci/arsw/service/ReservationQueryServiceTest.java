package edu.eci.arsw.service;

import edu.eci.arsw.domain.Reservation;
import edu.eci.arsw.dto.ReservationView;
import edu.eci.arsw.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationQueryServiceTest {

    @Mock
    private ReservationRepository repo;

    @Mock
    private ReservationViewAssembler assembler;

    @InjectMocks
    private ReservationQueryService service;

    @Test
    void listByTutorShouldUseRepositoryAndAssembler() {
        Reservation r1 = Reservation.builder().id("r1").build();
        Reservation r2 = Reservation.builder().id("r2").build();
        when(repo.findByTutorIdOrderByDateAscStartAsc("t1")).thenReturn(List.of(r1, r2));

        ReservationView v1 = ReservationView.builder().id("r1").build();
        ReservationView v2 = ReservationView.builder().id("r2").build();
        when(assembler.toView(r1)).thenReturn(v1);
        when(assembler.toView(r2)).thenReturn(v2);

        List<ReservationView> result = service.listByTutor("t1");

        assertEquals(List.of(v1, v2), result);
    }

    @Test
    void listByStudentShouldUseRepositoryAndAssembler() {
        Reservation r1 = Reservation.builder().id("r1").build();
        when(repo.findByStudentIdOrderByDateAscStartAsc("s1")).thenReturn(List.of(r1));

        ReservationView v1 = ReservationView.builder().id("r1").build();
        when(assembler.toView(r1)).thenReturn(v1);

        List<ReservationView> result = service.listByStudent("s1");

        assertEquals(1, result.size());
        assertSame(v1, result.get(0));
    }
}
