package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.internal.verification.Times;
import org.mockito.junit.jupiter.MockitoExtension;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TelegramBotUpdatesListenerTest {

    @Mock
    private TelegramBot telegramBot;

    @Mock
    private NotificationTaskRepository notificationTaskRepository;

    @Test
    void sendMessageTest() {
        NotificationTask notificationTask = NotificationTask.builder()
                .id(1L)
                .userId(2L)
                .text("test")
                .date(LocalDateTime.now())
                .build();
        SendMessage message = new SendMessage(notificationTask.getUserId(), notificationTask.getText());
        telegramBot.execute(message);
        verify(telegramBot, new Times(1)).execute(any(SendMessage.class));
    }

    @Test
    void sendMessageFalseTest() {
        NotificationTask notificationTask = NotificationTask.builder()
                .userId(1L)
                .date(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
                .text(null)
                .build();

        SendMessage message = new SendMessage(notificationTask.getUserId(), notificationTask.getText());
        telegramBot.execute(message);

        verify(notificationTaskRepository, new Times(0)).save(any(NotificationTask.class));
    }

    @Test
    void saveTimer() {
        NotificationTask notificationTask = NotificationTask.builder()
                .userId(1L)
                .date(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
                .text("/5min")
                .build();

        SendMessage message = new SendMessage(notificationTask.getUserId(), notificationTask.getText());
        telegramBot.execute(message);
        notificationTaskRepository.save(notificationTask);

        verify(notificationTaskRepository, new Times(1)).save(any(NotificationTask.class));

    }
    @Test
    void saveTimerFalse() {
        NotificationTask notificationTask = NotificationTask.builder()
                .userId(1L)
                .date(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
                .text("/null")
                .build();

        SendMessage message = new SendMessage(notificationTask.getUserId(), notificationTask.getText());
        telegramBot.execute(message);
        notificationTaskRepository.save(notificationTask);
        assertFalse(notificationTaskRepository.findById(notificationTask.getId()).isPresent());
    }

}