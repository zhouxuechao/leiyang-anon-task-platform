create table if not exists user_feedback (
  id bigint primary key auto_increment,
  user_id bigint not null,
  content varchar(1000) not null,
  contact varchar(128) null,
  status varchar(32) not null,
  created_at timestamp not null,
  updated_at timestamp not null,
  constraint fk_user_feedback_user foreign key (user_id) references user_account(id)
);

create index if not exists idx_user_feedback_created_at on user_feedback(created_at);
create index if not exists idx_user_feedback_status on user_feedback(status);
