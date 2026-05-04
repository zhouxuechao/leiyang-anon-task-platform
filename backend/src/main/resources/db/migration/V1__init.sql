-- Minimal MVP schema for anonymous task platform.
-- Note: H2 runs in MySQL compatibility mode in dev.

create table if not exists user_account (
  id bigint primary key auto_increment,
  open_id varchar(64) not null unique,
  nickname varchar(64) null,
  avatar varchar(512) null,
  status varchar(32) not null,
  credit_score int not null,
  created_at timestamp not null,
  updated_at timestamp not null
);

create table if not exists user_auth (
  id bigint primary key auto_increment,
  user_id bigint not null unique,
  real_name varchar(64) null,
  id_no_enc varchar(256) null,
  mobile varchar(32) null,
  pay_account varchar(128) null,
  status varchar(32) not null,
  created_at timestamp not null,
  updated_at timestamp not null,
  constraint fk_user_auth_user foreign key (user_id) references user_account(id)
);

create table if not exists task_publish (
  id bigint primary key auto_increment,
  task_no varchar(32) not null unique,
  publisher_id bigint not null,
  title varchar(128) not null,
  content text not null,
  location_text varchar(256) null,
  amount decimal(10,2) not null,
  total_slots int not null,
  accepted_slots int not null,
  deadline_at timestamp not null,
  proof_requirements varchar(256) null,
  status varchar(32) not null,
  reject_reason varchar(256) null,
  created_at timestamp not null,
  updated_at timestamp not null,
  constraint fk_task_publish_user foreign key (publisher_id) references user_account(id)
);

create table if not exists task_order (
  id bigint primary key auto_increment,
  order_no varchar(32) not null unique,
  task_id bigint not null,
  accept_user_id bigint not null,
  order_status varchar(32) not null,
  accept_time timestamp not null,
  submit_time timestamp null,
  audit_reason varchar(256) null,
  settled_time timestamp null,
  created_at timestamp not null,
  updated_at timestamp not null,
  constraint fk_task_order_task foreign key (task_id) references task_publish(id),
  constraint fk_task_order_user foreign key (accept_user_id) references user_account(id),
  unique (task_id, accept_user_id)
);

create table if not exists task_submit_proof (
  id bigint primary key auto_increment,
  order_id bigint not null,
  proof_type varchar(32) not null,
  proof_url varchar(1024) not null,
  remark varchar(256) null,
  created_at timestamp not null,
  constraint fk_task_submit_order foreign key (order_id) references task_order(id)
);

create table if not exists task_audit_log (
  id bigint primary key auto_increment,
  biz_type varchar(32) not null,
  biz_id bigint not null,
  result varchar(32) not null,
  reason varchar(256) null,
  auditor_id bigint null,
  auditor_type varchar(16) not null,
  audit_time timestamp not null
);

create table if not exists wallet_account (
  id bigint primary key auto_increment,
  user_id bigint not null unique,
  balance decimal(12,2) not null,
  frozen_amount decimal(12,2) not null,
  total_income decimal(12,2) not null,
  created_at timestamp not null,
  updated_at timestamp not null,
  constraint fk_wallet_user foreign key (user_id) references user_account(id)
);

create table if not exists wallet_flow (
  id bigint primary key auto_increment,
  user_id bigint not null,
  flow_type varchar(32) not null,
  amount decimal(12,2) not null,
  biz_no varchar(64) null,
  status varchar(32) not null,
  created_at timestamp not null,
  constraint fk_wallet_flow_user foreign key (user_id) references user_account(id)
);

create table if not exists withdraw_apply (
  id bigint primary key auto_increment,
  apply_no varchar(32) not null unique,
  user_id bigint not null,
  amount decimal(12,2) not null,
  channel varchar(32) not null,
  audit_status varchar(32) not null,
  audit_reason varchar(256) null,
  created_at timestamp not null,
  updated_at timestamp not null,
  constraint fk_withdraw_user foreign key (user_id) references user_account(id)
);

create table if not exists report_record (
  id bigint primary key auto_increment,
  report_no varchar(32) not null unique,
  reporter_id bigint not null,
  target_type varchar(32) not null,
  target_id bigint not null,
  reason varchar(256) not null,
  status varchar(32) not null,
  created_at timestamp not null,
  updated_at timestamp not null,
  constraint fk_report_user foreign key (reporter_id) references user_account(id)
);

create table if not exists sys_sensitive_word (
  id bigint primary key auto_increment,
  word varchar(64) not null unique,
  level int not null,
  action_type varchar(32) not null,
  status varchar(32) not null,
  created_at timestamp not null
);

create table if not exists user_message (
  id bigint primary key auto_increment,
  user_id bigint not null,
  msg_type varchar(32) not null,
  title varchar(128) not null,
  content varchar(512) not null,
  read_flag boolean not null,
  created_at timestamp not null,
  constraint fk_message_user foreign key (user_id) references user_account(id)
);

create table if not exists admin_user (
  id bigint primary key auto_increment,
  username varchar(64) not null unique,
  password_hash varchar(128) not null,
  status varchar(32) not null,
  created_at timestamp not null
);

-- Default admin: admin / admin123 (BCrypt)
insert into admin_user (username, password_hash, status, created_at)
select 'admin', '$2a$10$zF7dNQG6NIE.1d/R2VQ0luwT1iDTmMm2ds.1io8Dg8k8l/4bHqY4K', 'ACTIVE', current_timestamp
where not exists (select 1 from admin_user where username='admin');

