package com.cozyfireplace.server.rooms.roomDetails;

import com.cozyfireplace.server.rooms.Room;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Builder.Default
    private String situation = "Мастер пока не установил";

    @Builder.Default
    private String date = "Мастер пока не установил";

    @OneToOne
    @JoinColumn(name = "room_id", nullable = false, unique = true)
    private Room room;

    @Column(columnDefinition = "TEXT")
    private String lastSession;
}
