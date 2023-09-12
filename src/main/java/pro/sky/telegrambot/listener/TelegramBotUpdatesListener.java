package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendAnimation;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private final TelegramBot telegramBot;
    private final NotificationTaskRepository notificationTaskRepository;

    public TelegramBotUpdatesListener(TelegramBot telegramBot, NotificationTaskRepository notificationTaskRepository) {
        this.telegramBot = telegramBot;
        this.notificationTaskRepository = notificationTaskRepository;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            long chatId = update.message().chat().id();
            var name = update.message().chat().firstName();
            var gifUrl = "https://i.gifer.com/72vU.gif";
            Pattern pattern = Pattern.compile("([0-9.:\\s]{16})(\\s)([\\W+]+)");
            Matcher matcher;
            if (update.message().text() != null) {
                matcher = pattern.matcher(update.message().text());
            } else {
                System.out.println("Что пошло не так");
                return;
            }

            //if user sent /start
            if (update.message().text().equals("/start")) {
                SendMessage message = new SendMessage(chatId, "Привет, " + name + "\uD83D\uDE1D \n" +
                        "Пример напоминания для ввода: 01.01.2022 20:00 Сделать домашнюю работу ");
                SendAnimation sendAnimationRequest = new SendAnimation(chatId, gifUrl);
                telegramBot.execute(message);
                telegramBot.execute(sendAnimationRequest);
                //if user sent date it will be saved
            } else if (matcher.matches()) {
                saveDateAndTime(updates);
                SendMessage message = new SendMessage(chatId, "This message was saved successfully" +
                        " \uD83D\uDC4D\uD83D\uDE01");
                telegramBot.execute(message);
            }else {
                //Else send message
                SendMessage message = new SendMessage(chatId, "Извините, я вас не понимаю" +
                        " \uD83D\uDE21");
                telegramBot.execute(message);

            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    //method for saving date and time
    @Transactional
    public void saveDateAndTime(List<Update> updates) {
        updates.forEach(update -> {
            NotificationTask notificationTask = new NotificationTask();
            Long chatId = update.message().chat().id();
            notificationTask.setUserId(chatId);

            Pattern pattern = Pattern.compile("([0-9.:\\s]{16})(\\s)([\\W+]+)");
            Matcher matcher = pattern.matcher(update.message().text());
            while (matcher.find()) {
                String dateAsString = matcher.group(1); // Получаем дату и время
                try {
                    LocalDateTime localDateTime = LocalDateTime.parse(dateAsString, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                    notificationTask.setDate(localDateTime);
                    String text = matcher.group(3); // Получаем текст после даты и времени
                    notificationTask.setText(text); // Устанавливаем текст для задачи уведомления
                    notificationTaskRepository.save(notificationTask);
                } catch (DateTimeParseException e) {
                    // Если строка не может быть разобрана как дата, отправляем сообщение об ошибке пользователю
                    SendMessage message = new SendMessage(chatId, "Введенная дата недействительна. Пожалуйста, введите дату в формате 'dd.MM.yyyy HH:mm'");
                    telegramBot.execute(message);
                }
            }
        });
    }
    //Schedule method
    @Scheduled(cron = "0 0/1 * * * *")
    public void sendMessage() {
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