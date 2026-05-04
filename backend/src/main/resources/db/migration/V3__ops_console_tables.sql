-- Add tables for ops console: config, notice/banner, operation logs, attachments.

create table if not exists task_attach (
  id bigint primary key auto_increment,
  task_id bigint not null,
  file_url varchar(1024) not null,
  file_type varchar(32) not null,
  created_at timestamp not null,
  constraint fk_task_attach_task foreign key (task_id) references task_publish(id)
);

create table if not exists sys_notice (
  id bigint primary key auto_increment,
  title varchar(128) not null,
  content varchar(512) not null,
  status varchar(32) not null,
  sort_no int not null,
  created_at timestamp not null
);

create table if not exists sys_banner (
  id bigint primary key auto_increment,
  image_url varchar(1024) not null,
  link_url varchar(1024) null,
  status varchar(32) not null,
  sort_no int not null,
  created_at timestamp not null
);

create table if not exists sys_config (
  id bigint primary key auto_increment,
  cfg_key varchar(64) not null unique,
  cfg_value varchar(512) not null,
  remark varchar(256) null,
  updated_at timestamp not null
);

create table if not exists sys_op_log (
  id bigint primary key auto_increment,
  actor_type varchar(16) not null,
  actor_id bigint not null,
  method varchar(16) not null,
  path varchar(256) not null,
  ip varchar(64) null,
  user_agent varchar(256) null,
  ok_flag boolean not null,
  error_msg varchar(256) null,
  created_at timestamp not null
);

-- Seed some basic configs (defaults match application.yml)
insert into sys_config (cfg_key, cfg_value, remark, updated_at)
select 'limits.maxOngoingOrders', '3', 'Max ongoing orders per user', current_timestamp
where not exists (select 1 from sys_config where cfg_key='limits.maxOngoingOrders');

