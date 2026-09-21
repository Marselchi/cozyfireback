package com.cozyfireplace.server.sessions.participations;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.util.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ParticipationService {

    private final ParticipationRepository participationRepository;

    /**
     * Создать записи участия для сессии
     */
    @Transactional
    public void createParticipations(Long sessionId, List<Long> accountIds) {
        for (Long accountId : accountIds) {
            ParticipationId participationId = new ParticipationId(sessionId, accountId);
            Participation participation = Participation.builder()
                    .id(participationId)
                    .accepted(null) // null означает ожидание подтверждения
                    .build();
            participationRepository.save(participation);
        }
    }

    /**
     * Принять или отклонить приглашение
     */
    @Transactional
    public void acknowledgeParticipation(Long sessionId, Account account, boolean accepted) {
        ParticipationId participationId = new ParticipationId(sessionId, account.getId());
        Participation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new NotFoundException("Приглашение", "sessionId/accountId", sessionId + "/" + account.getId()));
        
        participation.setAccepted(accepted);
        participationRepository.save(participation);
    }

    /**
     * Сбросить статус accepted у всех участников сессии на null
     * Вызывается при обновлении времени сессии
     */
    @Transactional
    public void resetParticipationStatuses(Long sessionId) {
        // Находим все участия для данной сессии
        List<Participation> participations = participationRepository.findAllByIdSessionId(sessionId);
        for (Participation participation : participations) {
            participation.setAccepted(null);
        }
        participationRepository.saveAll(participations);
    }

    /**
     * Получить список участников сессии с их статусами
     */
    public List<Participation> findBySessionId(Long sessionId) {
        return participationRepository.findAllByIdSessionId(sessionId);
    }
}
