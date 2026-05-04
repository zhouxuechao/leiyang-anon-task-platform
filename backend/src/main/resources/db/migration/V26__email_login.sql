alter table user_auth add column if not exists email varchar(128) null;
alter table user_auth add column if not exists password_hash varchar(128) null;
create unique index if not exists ux_user_auth_email on user_auth(email);
