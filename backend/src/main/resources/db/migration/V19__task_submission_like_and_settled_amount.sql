alter table task_order add column if not exists settled_amount decimal(12,2);

create table if not exists task_submission_like (
  id bigint primary key auto_increment,
  order_id bigint not null,
  user_id bigint not null,
  created_at timestamp not null,
  updated_at timestamp not null,
  constraint fk_submission_like_order foreign key (order_id) references task_order(id),
  constraint fk_submission_like_user foreign key (user_id) references user_account(id),
  unique (order_id, user_id)
);
