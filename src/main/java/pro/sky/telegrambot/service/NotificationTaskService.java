package pro.sky.telegrambot.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NotificationTaskService {


    private static final Pattern PATTERN = Pattern.compile("([0-9.:\\s]{16})(\\s)([\\W+]+)");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(NotificationTaskService.class);
    private static NotificationTaskRepository notificationTaskRepository;

    private final TelegramBot telegramBot;

    public NotificationTaskService(NotificationTaskRepository notificationTaskRepository, TelegramBot telegramBot) {
        NotificationTaskService.notificationTaskRepository = notificationTaskRepository;
        this.telegramBot = telegramBot;
    }

    @Transactional
    public NotificationTask saveTimer(List<Update> updates, Matcher matcher1) {
        if (matcher1 == null) {
            logger.error("Message text is null");
            return null;
        }
        NotificationTask notificationTask = new NotificationTask();
        updates.forEach(update -> {
                    Long chatId = update.message().chat().id();
                    notificationTask.setUserId(chatId);
                    notificationTask.setDate(LocalDateTime
                            .now()
                            .plusMinutes(Integer
                                    .parseInt(matcher1
                                            .group(1)))
                            .truncatedTo(ChronoUnit.MINUTES)); //localDateTime now + matcher
                    String text = matcher1.group(1);
                    notificationTask.setText(text + " Минут прошло");
                    notificationTaskRepository.save(notificationTask);
                }
        );
        logger.info(notificationTask.toString());
        return notificationTask;
    }

    //method for saving date and time
    @Transactional
    public NotificationTask saveDateAndTime(List<Update> updates, Matcher matcher1) {
        if (matcher1 == null) {
            logger.error("Message text is null");
            return null;
        }
        NotificationTask notificationTask = new NotificationTask();
        updates.forEach(update -> {

            Long chatId = update.message().chat().id();
            notificationTask.setUserId(chatId);

            Matcher matcher = PATTERN.matcher(update.message().text());
            while (matcher.find()) {
                String dateAsString = matcher.group(1); // Получаем дату и время
                try {
                    LocalDateTime localDateTime = LocalDateTime.parse(dateAsString, FORMATTER);
                    notificationTask.setDate(localDateTime);
                    String text = matcher.group(3); // Получаем текст после даты и времени
                    notificationTask.setText(text); // Устанавливаем текст для задачи уведомления
                    notificationTaskRepository.save(notificationTask);
                } catch (DateTimeParseException e) {
                    // Если строка не может быть разобрана как дата, отправляем сообщение об ошибке пользователю
                    SendMessage message = new SendMessage(chatId, "Что-то пошло не так");
                    telegramBot.execute(message);
                }
            }
        });
        logger.info(notificationTask.toString());
        return notificationTask;
    }




}
