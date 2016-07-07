# --- !Ups

alter table silawet_message
alter column message varchar(2000)

# --- !Downs

alter table silawet_message
alter column message varchar(999)
