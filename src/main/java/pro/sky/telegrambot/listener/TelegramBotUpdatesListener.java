package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;
import pro.sky.telegrambot.service.NotificationTaskService;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private final TelegramBot telegramBot;
    private final NotificationTaskRepository notificationTaskRepository;

    private static final Pattern PATTERN = Pattern.compile("([0-9.:\\s]{16})(\\s)([\\W+]+)");
    private static final Pattern PATTERN1 = Pattern.compile("/(5|10|15|30)min$");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

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
            String smile = "\uD83D\uDC4D";
            Matcher matcher;
            Matcher matcher1;
            if (update.message().text() != null) {
                matcher = PATTERN.matcher(update.message().text());
                matcher1 = PATTERN1.matcher(update.message().text());
            } else {
                logger.error( "Message text is null");
                SendMessage message = new SendMessage(chatId, "Что пошло не так");
                telegramBot.execute(message);
                return;
            }

            //if user sent /start
            if (update.message().text().equals("/start")) {
                SendMessage message = new SendMessage(chatId, "Привет, " + name + "\uD83D\uDE1D \n" +
                        "Пример напоминания для ввода: " + LocalDateTime.now().format(FORMATTER) + " Сделать домашнюю работу "); // print time now
                telegramBot.execute(message);

                //if user sent date it will be saved
            }
            else {
                if (matcher.matches()) {
                    NotificationTaskService notificationTaskService = new NotificationTaskService(notificationTaskRepository, telegramBot);
                    NotificationTask notificationTask = notificationTaskService.saveDateAndTime(updates, matcher);
                    notificationTaskRepository.save(notificationTask);
                    SendMessage message = new SendMessage(chatId, " Будильник сработает "
                            + matcher.group(1) + " " + matcher.group(2) + smile);
                    telegramBot.execute(message);

                } else if (matcher1.matches()) {//  three buttons set timer to /5min /10min /15min /30min
                    NotificationTaskService notificationTaskService = new NotificationTaskService(notificationTaskRepository, telegramBot);
                    NotificationTask notificationTask = notificationTaskService.saveTimer(updates, matcher1);
                    notificationTaskRepository.save(notificationTask);
                    SendMessage message = new SendMessage(chatId, "Будильник успешно поставлен" + smile);
                    telegramBot.execute(message);

                } else {
                    //Else send message
                    SendMessage message = new SendMessage(chatId, "Извините, я вас не понимаю" +
                            " \uD83D\uDE21");
                    telegramBot.execute(message);

                }
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

}

