
# --- !Ups

create table silawet_message (
  id                        BIGSERIAL not null,
  silawet_id                varchar(999),
  message                   varchar(999),
  signature                 varchar(999),
  authored_by               varchar(999),
  authored_at               varchar(255),
  created_at                timestamp,
  year                      integer,
  month                     integer,
  day                       integer,
  hour                      integer,
  constraint pk_silawet_message primary key (id))
;

create table silawet_user (
  id                        BIGSERIAL not null,
  username                  varchar(255),
  password                  varchar(512),
  private_key               varchar(2000),
  public_key                varchar(2000),
  created_date              timestamp,
  hidden                    boolean,
  constraint pk_silawet_user primary key (id))
;

create sequence silawet_message_seq;

create sequence silawet_user_seq;




# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists silawet_message;

drop table if exists silawet_user;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists silawet_message_seq;

drop sequence if exists silawet_user_seq;

