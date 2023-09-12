-- liquibase formatted sql

-- changeset Dmitry:1
create schema if not exists telegram_bot;

-- changeset Dmitry:2
create table if not exists notification_task(
    id bigSerial primary key ,
    user_id bigInt not null,
    text text not null ,
    date timestamp not null
);