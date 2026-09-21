package com.cozyfireplace.server.invitations;

import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.roles.Role;
import com.cozyfireplace.server.rooms.Room;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.Set;

@Entity
@Table(name = "invitations")
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
//No single use codes coz idk
public class Invitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;
}
