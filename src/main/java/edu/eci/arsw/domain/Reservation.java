package edu.eci.arsw.domain;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalTime;

/** Representa una reserva realizada por un estudiante con un tutor */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("reservations")
@CompoundIndex(name = "uniq_student_slot", def = "{ 'studentId':1, 'date':1, 'start':1 }", unique = true)
@CompoundIndex(name = "uniq_tutor_slot", def = "{ 'tutorId':1, 'date':1, 'start':1 }", unique = true)
public class Reservation {
    @Id
    private String id;
    private String tutorId;
    private String studentId;
    private LocalDate date;
    private LocalTime start;
    private LocalTime end;
    private ReservationStatus status;
    @CreatedDate
    private java.time.Instant createdAt;
    @LastModifiedDate
    private java.time.Instant updatedAt;
    @Version
    private Long version;
}
