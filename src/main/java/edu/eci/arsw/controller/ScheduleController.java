package edu.eci.arsw.controller;

import edu.eci.arsw.dto.ScheduleCell;
import edu.eci.arsw.security.AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import edu.eci.arsw.service.ScheduleService;
import java.time.LocalDate;
import java.util.List;

/**
 * Controlador para manejar las solicitudes relacionadas con el horario de
 * tutores
 */
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
@Slf4j
public class ScheduleController {

    private final ScheduleService service;
    private final AuthorizationService authz;

    /**
     * Obtener el horario semanal de un tutor específico
     * 
     * @param authorization Token de autorización
     * @param tutorId       ID del tutor
     * @param weekStart     Fecha de inicio de la semana
     * @return Lista de celdas del horario
     */
    @GetMapping("/tutor/{tutorId}")
    public ResponseEntity<List<ScheduleCell>> week(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("tutorId") String tutorId,
            @RequestParam("weekStart") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        authz.requireRole(authorization, "STUDENT", "TUTOR");
        List<ScheduleCell> schedule = service.weekForTutor(tutorId, weekStart);
        return ResponseEntity.ok(schedule);
    }
}
