package com.cozyfireplace.server.accounts.stats;


import com.cozyfireplace.server.rooms.Room;
import com.cozyfireplace.server.rooms.RoomRepository;
import com.cozyfireplace.server.util.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillService {
    private final SkillRepository skillRepository;
    private final RoomRepository roomRepository;
    private final SkillMapper skillMapper;

    public List<SkillListResponse> getSkills(Long roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Room Not Found"));
        if (room.getGameSystem() == null) {
            throw new NotFoundException("Room Game System Not Found");
        }
        List<Skill> skills = skillRepository.findAllBySystem(room.getGameSystem());
        return skills.stream().map(skillMapper::toSkillListResponse).toList();
    }
}
