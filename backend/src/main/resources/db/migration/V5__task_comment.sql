create table if not exists task_comment (
  id bigint primary key auto_increment,
  task_id bigint not null,
  user_id bigint not null,
  content varchar(512) not null,
  created_at timestamp not null,
  updated_at timestamp not null,
  constraint fk_task_comment_task foreign key (task_id) references task_publish(id),
  constraint fk_task_comment_user foreign key (user_id) references user_account(id)
);

