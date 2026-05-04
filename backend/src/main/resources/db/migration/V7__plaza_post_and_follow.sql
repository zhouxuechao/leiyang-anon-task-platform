alter table user_account add column if not exists gender varchar(16) null;

update user_account set gender = 'MALE' where open_id in ('seed-taker-001', 'seed-taker-003', 'seed-taker-005', 'seed-taker-007');
update user_account set gender = 'FEMALE' where open_id in ('seed-taker-002', 'seed-taker-004', 'seed-taker-006', 'seed-publisher-001');

create table if not exists plaza_post (
  id bigint primary key auto_increment,
  author_id bigint not null,
  content varchar(500) not null,
  gender varchar(16) not null,
  like_count int not null default 0,
  comment_count int not null default 0,
  created_at timestamp not null,
  updated_at timestamp not null,
  constraint fk_plaza_post_author foreign key (author_id) references user_account(id)
);

create table if not exists plaza_post_image (
  id bigint primary key auto_increment,
  post_id bigint not null,
  image_url varchar(1024) not null,
  sort_no int not null,
  created_at timestamp not null,
  constraint fk_plaza_post_image_post foreign key (post_id) references plaza_post(id)
);

create table if not exists plaza_follow (
  id bigint primary key auto_increment,
  user_id bigint not null,
  target_user_id bigint not null,
  created_at timestamp not null,
  constraint fk_plaza_follow_user foreign key (user_id) references user_account(id),
  constraint fk_plaza_follow_target foreign key (target_user_id) references user_account(id),
  unique (user_id, target_user_id)
);

