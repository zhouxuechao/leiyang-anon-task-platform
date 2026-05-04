create table if not exists ai_music_comment (
  id bigint primary key auto_increment,
  music_id bigint not null,
  user_id bigint not null,
  content varchar(512) not null,
  created_at timestamp not null,
  constraint fk_music_comment_music foreign key (music_id) references ai_music_job(id),
  constraint fk_music_comment_user foreign key (user_id) references user_account(id)
);
