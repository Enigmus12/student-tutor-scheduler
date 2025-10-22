package edu.eci.arsw.dto;

import lombok.*;
/** Perfil público de un usuario */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicProfile {
    private String id;
    private String sub;
    private String name;
    private String email;
    private String avatarUrl;
}
