// src/main/java/co/eci/uplearn/reservation/domain/Availability.java
package co.eci.uplearn.reservation.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@Document(collection = "availability")
@CompoundIndex(name = "tutor_day_idx", def = "{'tutorId':1,'day':1,'start':1,'end':1}")
public class Availability {

  @Id
  private String id;

  private String tutorId;

  // Día de la disponibilidad
  private LocalDate day;

  // Rangos dentro del día
  private LocalTime start;
  private LocalTime end;
}
