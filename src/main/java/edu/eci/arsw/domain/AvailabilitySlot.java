package edu.eci.arsw.domain;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalTime;
/** Representa un intervalo de disponibilidad de un tutor */
@Data @Builder @AllArgsConstructor @NoArgsConstructor
@Document("availability_slots")
@CompoundIndex(name="uniq_av", def="{ 'tutorId':1, 'date':1, 'start':1 }", unique = true)
public class AvailabilitySlot {
    @Id
    private String id;
    private String tutorId;
    private LocalDate date;
    private LocalTime start;
    private LocalTime end;
    @CreatedDate
    private java.time.Instant createdAt;
    @LastModifiedDate
    private java.time.Instant updatedAt;
}
