package com.cozyfireplace.server.notifications.listener;

import com.cozyfireplace.server.notifications.RoomNotificationPublisher;
import com.cozyfireplace.server.notifications.dto.NotificationEventCode;
import com.cozyfireplace.server.notifications.dto.NotificationType;
import com.cozyfireplace.server.notifications.dto.RoomNotificationCommand;
import com.cozyfireplace.server.notifications.dto.TargetType;
import com.cozyfireplace.server.notifications.event.CharacterEvent;
import com.cozyfireplace.server.notifications.event.QAEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
public class QuestionAnswerNotificationListener {
    private final RoomNotificationPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(QAEvent event) {
        RoomNotificationCommand command = switch (event.type()) {
            case CREATE -> buildCreateQuestionCommand(event);
            case QUESTION_ANSWER -> buildQuestionAnswerCommand(event);
            case ANSWER -> buildAnswerAnswerCommand(event);
            default -> null;
        };

        if (command != null) {
            publisher.publish(command);
        }
    }

    private RoomNotificationCommand buildCreateQuestionCommand(QAEvent event) {
        return RoomNotificationCommand.builder()
                .type(NotificationType.QA)
                .eventCode(NotificationEventCode.QA_NEW_QUESTION)
                .title("Новый вопрос")
                .body(String.format("Создан новый вопрос в %s от: %s", event.roomName(), event.authorName()))
                .targetId(event.questionId())
                .targetType(TargetType.QUESTION)
                .roomId(event.roomId())
                .roomUrl(event.roomUrl())
                .authorAccountId(event.authorAccountId())
                .build();
    }

    private RoomNotificationCommand buildQuestionAnswerCommand(QAEvent event) {
        return RoomNotificationCommand.builder()
                .type(NotificationType.QA)
                .eventCode(NotificationEventCode.QA_ANSWERED)
                .title("Ответ на вопрос")
                .body(String.format("На ваш вопрос в %s ответил: %s%s",
                        event.roomName(),
                        event.authorName(),
                        event.byDm() ? " (DM)" : ""))
                .targetId(event.questionId())
                .targetType(TargetType.QUESTION)
                .roomId(event.roomId())
                .roomUrl(event.roomUrl())
                .authorAccountId(event.authorAccountId())
                .recipientId(event.replyToAccountId())
                .build();
    }

    private RoomNotificationCommand buildAnswerAnswerCommand(QAEvent event) {
        return RoomNotificationCommand.builder()
                .type(NotificationType.QA)
                .eventCode(NotificationEventCode.QA_ANSWER_TO_ANSWER)
                .title("Ответ на ответ")
                .body(String.format("На ваш ответ ответил: %s%s",
                        event.authorName(),
                        event.byDm() ? " (DM)" : ""))
                .targetId(event.questionId())
                .targetType(TargetType.QUESTION)
                .answerId(event.answerId())
                .roomId(event.roomId())
                .roomUrl(event.roomUrl())
                .authorAccountId(event.authorAccountId())
                .recipientId(event.replyToAccountId())
                .build();
    }
}
