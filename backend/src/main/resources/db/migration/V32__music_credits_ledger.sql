create table if not exists music_credit_ledger (
  id bigint primary key auto_increment,
  user_id bigint not null,
  change_amount int not null,
  biz_type varchar(32) not null,
  biz_no varchar(64) null,
  created_at timestamp not null,
  constraint fk_music_credit_user foreign key (user_id) references user_account(id)
);
