alter table withdraw_apply add column if not exists paid_proof_url varchar(1024);
alter table withdraw_apply add column if not exists pay_remark varchar(256);
alter table withdraw_apply add column if not exists paid_at timestamp;

