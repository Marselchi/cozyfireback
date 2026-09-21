package com.cozyfireplace.server.auth.profile;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "profiles", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
})
@NoArgsConstructor
@Builder
@AllArgsConstructor
@Getter
@Setter
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Email
    @Column(columnDefinition = "TEXT")
    private String email;

    @Size(min = 3, max = 20)
    @Column(columnDefinition = "TEXT")
    private String username;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String password;
}