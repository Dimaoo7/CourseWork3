package pro.sky.telegrambot.components;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class SendMessageTask {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(SendMessageTask.class);
    private final NotificationTaskRepository notificationTaskRepository;

    private final TelegramBot telegramBot;

    public SendMessageTask(NotificationTaskRepository notificationTaskRepository, TelegramBot telegramBot) {
        this.notificationTaskRepository = notificationTaskRepository;
        this.telegramBot = telegramBot;
    }

    @Scheduled(cron = "0 0/1 * * * *")
    public void sendMessage() {
        allByDate(notificationTaskRepository, logger, telegramBot);
    }

    public static void allByDate(NotificationTaskRepository notificationTaskRepository, Logger logger, TelegramBot telegramBot) {
        List<NotificationTask> allByDate = notificationTaskRepository.findAllByDate(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
        logger.info("All tasks: {}", allByDate);
        allByDate.forEach(notificationTask -> {
            SendMessage message = new SendMessage(notificationTask.getUserId(), notificationTask.getText());
            SendResponse sendResponse = telegramBot.execute(message);
            if (sendResponse.isOk()) {
                notificationTaskRepository.deleteById(notificationTask.getId());
            } else {
                logger.info("Error: {}", sendResponse.description());
            }
        });
    }
}

