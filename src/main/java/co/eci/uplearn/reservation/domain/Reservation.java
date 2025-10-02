package co.eci.uplearn.reservation.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.*;

@Getter @Setter @ToString
@Builder @AllArgsConstructor @NoArgsConstructor
@Document(collection = "reservations")
public class Reservation {
    @Id
    private String id;
    private String studentId;
    private String tutorId;
    private LocalDate day;
    private LocalTime start;
    private LocalTime end;
    private Instant createdAt;
    @Builder.Default
    private Status status = Status.CONFIRMED;

    public enum Status { CONFIRMED, CANCELLED }
}
