package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
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
            Pattern pattern = Pattern.compile("([0-9.:\\s]{16})(\\s)([\\W+]+)");
            Pattern pattern1 = Pattern.compile("/(5|10|15|30)min$"); // only 5 10 15 30 тут что то
            Matcher matcher;
            Matcher matcher1;
            if (update.message().text() != null) {
                matcher = pattern.matcher(update.message().text());
                matcher1 = pattern1.matcher(update.message().text());
            } else {
                System.out.println("Что пошло не так");
                return;
            }

            //if user sent /start
            if (update.message().text().equals("/start")) {
                SendMessage message = new SendMessage(chatId, "Привет, " + name + "\uD83D\uDE1D \n" +
                        "Пример напоминания для ввода: " + LocalDateTime.now().format(DateTimeFormatter
                        .ofPattern("dd.MM.yyyy HH:mm")) + " Сделать домашнюю работу "); // print time now
                telegramBot.execute(message);

                //if user sent date it will be saved
            } else if (matcher.matches()) {
                NotificationTask notificationTask = saveDateAndTime(updates);
                notificationTaskRepository.save(notificationTask);

            } else if (matcher1.matches()) {//  three buttons set timer to /5min /10min /15min /30min
                NotificationTask notificationTask = saveTimer(updates, matcher1);
                notificationTaskRepository.save(notificationTask);

            } else {
                //Else send message
                SendMessage message = new SendMessage(chatId, "Извините, я вас не понимаю" +
                        " \uD83D\uDE21");
                telegramBot.execute(message);

            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    @Transactional
    public NotificationTask saveTimer(List<Update> updates, Matcher matcher1) {
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
        System.out.println(notificationTask);
        return notificationTask;
    }

    //method for saving date and time
    @Transactional
    public NotificationTask saveDateAndTime(List<Update> updates) {
        NotificationTask notificationTask = new NotificationTask();
        updates.forEach(update -> {

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
                    SendMessage message = new SendMessage(chatId, "Введенная дата недействительна. Пожалуйста," +
                            " введите дату в формате 'dd.MM.yyyy HH:mm'");
                    telegramBot.execute(message);
                }
            }
        });
        System.out.println(notificationTask);
        return notificationTask;
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