create table if not exists plaza_post_like (
  id bigint primary key auto_increment,
  post_id bigint not null,
  user_id bigint not null,
  created_at timestamp not null,
  updated_at timestamp not null,
  constraint fk_plaza_like_post foreign key (post_id) references plaza_post(id),
  constraint fk_plaza_like_user foreign key (user_id) references user_account(id),
  unique (post_id, user_id)
);

create table if not exists plaza_post_comment (
  id bigint primary key auto_increment,
  post_id bigint not null,
  user_id bigint not null,
  content varchar(300) not null,
  created_at timestamp not null,
  updated_at timestamp not null,
  constraint fk_plaza_comment_post foreign key (post_id) references plaza_post(id),
  constraint fk_plaza_comment_user foreign key (user_id) references user_account(id)
);
