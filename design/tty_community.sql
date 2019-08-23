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
    last_login_time timestamp not null,
    email text not null
);

create table user_detail(
    _id integer primary key auto_increment,
    id varchar(32) not null unique,
    portrait text not null,
    follower blob not null,
    following blob not null,
    personal_signature text not null,
    account_status text not null,
    user_group integer not null default 0,
    exp integer not null default 20,
    data blob,
    topic json not null,
    school text,
    log longblob not null,
    settings json
);

create table blog(
    _id integer primary key auto_increment,
    blog_id varchar(32) not null unique,
    author_id varchar(32) not null,
    type integer default 0 not null,
    title text not null,
    introduction text not null,
    content blob not null,
    tag text not null,
    comments json not null,
    likes json not null,
    last_edit_time datetime not null,
    last_active_time datetime not null,
    status integer not null,
    data blob,
    log longblob not null
);

create table topic(
    _id integer auto_increment primary key,
    topic_id varchar(32) not null unique,
    name varchar(32) not null unique,
    introduction text not null,
    picture text not null,
    follower json not null,
    parent varchar(32) not null,
    admin varchar(32) not null,
    last_active_time timestamp not null,
    status integer not null,
    log longblob not null
);
