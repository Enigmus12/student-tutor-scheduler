// ReservationQueryController.java
package edu.eci.arsw.controller;

import edu.eci.arsw.dto.ReservationView;
import edu.eci.arsw.service.ReservationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/**
 * Controlador para manejar las consultas de reservas
 */
@RestController
@RequestMapping("/Api-reservation")
@RequiredArgsConstructor
public class ReservationQueryController {

    private final ReservationQueryService service;
    /**
     * Obtener las reservas por tutor
     */
    @GetMapping("/by-tutor/{tutorId}")
    public List<ReservationView> byTutor(@PathVariable String tutorId) {
        return service.listByTutor(tutorId);
    }
    /**
     * Obtener las reservas por estudiante
     */
    @GetMapping("/by-student/{studentId}")
    public List<ReservationView> byStudent(@PathVariable String studentId) {
        return service.listByStudent(studentId);
    }
}
