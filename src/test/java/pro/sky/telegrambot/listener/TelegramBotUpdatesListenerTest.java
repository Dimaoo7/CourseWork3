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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TelegramBotUpdatesListenerTest {

    @Mock
    private TelegramBot telegramBot;

    @Mock
    private NotificationTaskRepository notificationTaskRepository;

    @Test
    void processTest() {
        SendMessage sendMessage = new SendMessage( 1L, "test");
        telegramBot.execute(sendMessage);
        verify(telegramBot, new Times(1)).execute(any(SendMessage.class));
    }

    @Test
    void saveDateAndTimeTest() {
        NotificationTask notificationTask = new NotificationTask();
        notificationTaskRepository.save(notificationTask);

        assertNotNull(notificationTask);
    }

    @Test
    void sendMessageTest() {
        NotificationTask notificationTask = NotificationTask.builder()
                .date(LocalDateTime.now())
                .text("test")
                .build();
        notificationTaskRepository.save(notificationTask);
        System.out.println(notificationTask);

        assertNotNull(notificationTask);
    }
}