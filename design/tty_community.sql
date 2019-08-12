create database tty_community;
alter database tty_community character set utf8mb4;
use tty_community;

create table user(
    _id integer primary key auto_increment,
    id varchar(32) not null unique,
    nickname varchar(32) not null unique,
    token text not null,
    password text not null,
    last_login_ip text not null,
    last_login_time text not null,
    email text not null
);

create table user_detail(
    _id integer primary key auto_increment,
    id varchar(32) not null unique,
    portrait text not null,
    follower text not null,
    following text not null,
    personal_signature text not null,
    account_status text not null,
    user_group integer not null default 0,
    exp integer not null default 20,
    data blob,
    log blob not null,
    settings json #todo
);

create table blog(
    _id integer primary key auto_increment,
    blog_id varchar(32) not null unique,
    author_id varchar(32) not null,
    title text not null,
    introduction text not null,
    content blob not null,
    tag text not null,
    comment blob not null,
    likes blob not null,
    last_edit_time datetime not null,
    last_active_time datetime not null, #todo
    status text not null,
    data blob,
    log blob not null
);
