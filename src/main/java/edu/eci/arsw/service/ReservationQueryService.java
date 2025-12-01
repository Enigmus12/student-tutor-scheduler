// ReservationQueryService.java
package edu.eci.arsw.service;

import edu.eci.arsw.domain.Reservation;
import edu.eci.arsw.dto.ReservationView;
import edu.eci.arsw.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio para gestionar consultas de reservas
 */
@Service
@RequiredArgsConstructor
public class ReservationQueryService {

    private final ReservationRepository reservationRepository;
    private final ReservationViewAssembler assembler;

    /**
     * Listar reservas por tutor
     * 
     * @param tutorId ID del tutor
     * @return Lista de vistas de reservas del tutor
     */
    public List<ReservationView> listByTutor(String tutorId) {
        List<Reservation> rs = reservationRepository.findByTutorIdOrderByDateAscStartAsc(tutorId);
        return rs.stream().map(assembler::toView).toList();
    }

    /**
     * Listar reservas por estudiante
     * 
     * @param studentId ID del estudiante
     * @return Lista de vistas de reservas del estudiante
     */
    public List<ReservationView> listByStudent(String studentId) {
        List<Reservation> rs = reservationRepository.findByStudentIdOrderByDateAscStartAsc(studentId);
        return rs.stream().map(assembler::toView).toList();
    }
}
