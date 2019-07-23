create database tty_community;
alter database tty_community character set utf8mb4;
create table user(
    _id integer primary key auto_increment,
    id text not null unique,
    nickname text not null unique,
    token text not null,
    password text not null,
    last_login_ip text not null,
    last_login_time text not null,
    email text not null,
    log blob not null
);
create table blog(
    _id integer primary key auto_increment,
    title text not null,
    introduction text not null,
    content blob not null,
    tag text not null,
    comment blob not null,
    likes blob not null,
    last_edit_time datetime not null,
    data blob,
    log blob not null
);