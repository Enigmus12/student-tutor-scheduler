package co.eci.uplearn.reservation.web;

import co.eci.uplearn.reservation.domain.Availability;
import co.eci.uplearn.reservation.domain.Reservation;
import co.eci.uplearn.reservation.service.AvailabilityService;
import co.eci.uplearn.reservation.service.ReservationService;
import co.eci.uplearn.reservation.web.dto.AvailabilitySlot;
import co.eci.uplearn.reservation.web.dto.CreateReservationRequest;
import co.eci.uplearn.reservation.web.dto.ReservationResponse;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ReservationController {
  private final ReservationService reservations;
  private final AvailabilityService availability;

  public ReservationController(ReservationService reservations, AvailabilityService availability) {
    this.reservations = reservations;
    this.availability = availability;
  }

  // --- Reservations ---

  @PostMapping("/reservations")
  public ResponseEntity<ReservationResponse> create(
      @RequestBody @Valid CreateReservationRequest req,
      Authentication auth,
      @RequestHeader(value = "X-App-Role", required = false) String appRole,
      @RequestHeader(value = "X-Subject", required = false) String subjectFromHeader // 👈 nuevo
  ) {
    System.out.println("POST /reservations  X-App-Role=" + appRole +
      "  X-Subject=" + subjectFromHeader +
      "  auth=" + (auth != null ? auth.getName() : "null"));
    // 1) Debe venir como student (o tener el rol en el token)
    boolean isStudentByHeader = "student".equalsIgnoreCase(appRole);
    if (!(isStudentByHeader || hasRole(auth, "STUDENT"))) {
      throw new RuntimeException("Forbidden");
    }

    // 2) Necesitamos el sub: del Authentication o del header de fallback
    String studentId = extractSub(auth);
    if (studentId == null || studentId.isBlank()) {
      studentId = (subjectFromHeader != null && !subjectFromHeader.isBlank()) ? subjectFromHeader : null;
    }
    if (studentId == null) {
      throw new RuntimeException("Forbidden");
    }

    Reservation r = reservations.create(studentId, req.tutorId(), req.day(), req.start(), req.end());
    return ResponseEntity.ok(toResp(r));
  }



  @GetMapping("/reservations/by-student/{studentId}")
  public List<ReservationResponse> byStudent(@PathVariable String studentId, Authentication auth) {
    // Solo el dueño o ADMIN
    if (!studentId.equals(extractSub(auth)) && !hasRole(auth,"ADMIN")) {
      throw new RuntimeException("Forbidden");
    }
    return reservations.byStudent(studentId).stream().map(this::toResp).toList();
  }

  @GetMapping("/reservations/by-tutor/{tutorId}")
  public List<ReservationResponse> byTutor(@PathVariable String tutorId, Authentication auth) {
    // TUTOR puede ver sólo si es su id, ADMIN puede ver cualquier
    if (!hasRole(auth,"ADMIN") && !(hasRole(auth,"TUTOR") && tutorId.equals(extractSub(auth)))) {
      throw new RuntimeException("Forbidden");
    }
    return reservations.byTutor(tutorId).stream().map(this::toResp).toList();
  }

  @GetMapping("/reservations/by-tutor/{tutorId}/day/{day}")
  public List<ReservationResponse> byTutorDay(
      @PathVariable String tutorId,
      @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day,
      Authentication auth
  ) {
    if (!hasRole(auth,"ADMIN") && !(hasRole(auth,"TUTOR") && tutorId.equals(extractSub(auth)))) {
      throw new RuntimeException("Forbidden");
    }
    return reservations.byTutorAndDay(tutorId, day).stream().map(this::toResp).toList();
  }

  // --- Availability (persisted) ---
@GetMapping("/whoami")
public Map<String,Object> whoami(Authentication auth) {
  Map<String,Object> m = new LinkedHashMap<>();
  m.put("sub", extractSub(auth));
  m.put("authorities", auth == null ? List.of() : auth.getAuthorities());
  if (auth != null && auth.getPrincipal() instanceof co.eci.uplearn.reservation.security.JwtAuthFilter.UserPrincipal up) {
    m.put("email", up.getEmail());
    m.put("roles_raw", up.getRoles()); // lo que LooseJwtDecoder extrajo
  }
  return m;
}
  @PutMapping("/availability/{tutorId}/day/{day}")
  public List<AvailabilitySlot> setAvailability(
      @PathVariable String tutorId,
      @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day,
      @RequestBody List<AvailabilitySlot> slots,
      Authentication auth
  ) {
    if (!hasRole(auth,"ADMIN") && !(hasRole(auth,"TUTOR") && tutorId.equals(extractSub(auth)))) {
        throw new RuntimeException("Forbidden");
    }

    List<Availability> list = new ArrayList<>();
    for (AvailabilitySlot s : slots) {
      list.add(Availability.builder()
          .tutorId(tutorId)
          .day(day)
          .start(s.start())
          .end(s.end())
          .build());
    }
    return availability.setDayAvailability(tutorId, day, list).stream()
        .map(a -> new AvailabilitySlot(a.getStart(), a.getEnd(), true))
        .toList();
  }

  @GetMapping("/availability/{tutorId}/day/{day}")
  public List<AvailabilitySlot> getAvailability(
      @PathVariable String tutorId,
      @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day
  ) {
    return availability.getDayAvailability(tutorId, day).stream()
        .map(a -> new AvailabilitySlot(a.getStart(), a.getEnd(), true))
        .toList();
  }

  // --- Computed weekly grid for the UI (no auth needed para pintar slots) ---

  @GetMapping("/availability/week")
  public Map<String, List<AvailabilitySlot>> weekGrid(
      @RequestParam String tutorId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
      @RequestParam int slotMinutes,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime dayStart,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime dayEnd
  ) {
    Map<String, List<AvailabilitySlot>> result = new LinkedHashMap<>();
    for (int i = 0; i < 7; i++) {
      LocalDate day = weekStart.plusDays(i);
      List<AvailabilitySlot> slots = new ArrayList<>();
      LocalTime t = dayStart;
      while (!t.plusMinutes(slotMinutes).isAfter(dayEnd)) {
        final LocalTime currentTime = t;
        final LocalTime tEnd = t.plusMinutes(slotMinutes);
        boolean covered = availability.isCoveredByAvailability(tutorId, day, currentTime, tEnd);
        boolean hasRes = reservations.byTutorAndDay(tutorId, day).stream()
            .anyMatch(r -> r.getStart().isBefore(tEnd) && r.getEnd().isAfter(currentTime));
        slots.add(new AvailabilitySlot(currentTime, tEnd, covered && !hasRes));
        t = tEnd;
      }
      result.put(day.toString(), slots);
    }
    return result;
  }

  private ReservationResponse toResp(Reservation r) {
    return new ReservationResponse(
        r.getId(), r.getStudentId(), r.getTutorId(), r.getDay(), r.getStart(), r.getEnd(), r.getStatus().name()
    );
  }

  private String extractSub(Authentication auth) {
    if (auth == null || auth.getPrincipal() == null) return null;
    if (auth.getPrincipal() instanceof co.eci.uplearn.reservation.security.JwtAuthFilter.UserPrincipal up) {
      return up.getSub();
    }
    return null;
  }

  private boolean hasRole(Authentication auth, String role) {
    if (auth == null) return false;
    String want = ("ROLE_" + role.toUpperCase());
    for (GrantedAuthority ga : auth.getAuthorities()) {
      if (want.equals(ga.getAuthority())) return true;
    }
    return false;
  }
}
