-- liquibase formatted sql

-- changeset Dmitry:1
CREATE TABLE IF NOT EXISTS `notification_task` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `chat_id` bigint NOT NULL,
    `date_time` timestamp NOT NULL,
    PRIMARY KEY (`id`)
)