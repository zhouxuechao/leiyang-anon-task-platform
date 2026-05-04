create table if not exists recharge_order (
  id bigint primary key auto_increment,
  out_trade_no varchar(64) not null unique,
  user_id bigint not null,
  amount_fen int not null,
  status varchar(32) not null,
  wx_transaction_id varchar(64) null,
  created_at timestamp not null,
  updated_at timestamp not null,
  constraint fk_recharge_order_user foreign key (user_id) references user_account(id)
);
