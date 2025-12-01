package edu.eci.arsw.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Referencia a un usuario (estudiante o tutor) */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRef {
    private String id;
    private String email;
    private String name;
}
