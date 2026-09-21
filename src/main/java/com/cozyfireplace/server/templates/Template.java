package com.cozyfireplace.server.templates;


import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.rooms.Room;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "templates")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Template {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(columnDefinition = "TEXT",nullable = false)
    private String name;

    @NotBlank
    @Column(columnDefinition = "TEXT",nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account author;

}
