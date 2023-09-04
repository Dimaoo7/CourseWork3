package pro.sky.telegrambot.model;


import javax.persistence.*;

@Entity
public class NotificationTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "chat_id")
    private long chatId;
    private long dateTime;

    public NotificationTask(long chatId, long dateTime) {
        this.chatId = chatId;
        this.dateTime = dateTime;
    }

    public NotificationTask() {

    }

    public long getChatId() {
        return chatId;
    }
    public long getDateTime() {
        return dateTime;
    }
    public void setChatId(long chatId) {
        this.chatId = chatId;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
