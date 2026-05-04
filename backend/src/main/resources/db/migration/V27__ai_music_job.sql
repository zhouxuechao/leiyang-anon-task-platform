create table if not exists ai_music_job (
  id bigint primary key auto_increment,
  user_id bigint not null,
  title varchar(128) not null,
  prompt text not null,
  style varchar(512) null,
  custom_mode boolean not null,
  instrumental boolean not null,
  lang varchar(32) null,
  status varchar(32) not null,
  suno_task_id varchar(128) null,
  audio_url varchar(2048) null,
  video_url varchar(2048) null,
  image_url varchar(2048) null,
  duration varchar(32) null,
  raw_response text null,
  error_message varchar(1024) null,
  created_at timestamp not null,
  updated_at timestamp not null,
  constraint fk_ai_music_job_user foreign key (user_id) references user_account(id)
);

create index if not exists idx_ai_music_job_user_created on ai_music_job(user_id, created_at);

insert into sys_config (cfg_key, cfg_value, remark, updated_at)
select 'suno.api.enabled', 'false', 'Suno API 是否启用', current_timestamp
where not exists (select 1 from sys_config where cfg_key = 'suno.api.enabled');

insert into sys_config (cfg_key, cfg_value, remark, updated_at)
select 'suno.api.base_url', 'https://api.sunoapi.org', 'Suno API Base URL', current_timestamp
where not exists (select 1 from sys_config where cfg_key = 'suno.api.base_url');

insert into sys_config (cfg_key, cfg_value, remark, updated_at)
select 'suno.api.model', 'V4_5ALL', 'Suno API 模型', current_timestamp
where not exists (select 1 from sys_config where cfg_key = 'suno.api.model');

insert into sys_config (cfg_key, cfg_value, remark, updated_at)
select 'suno.api.timeout_ms', '30000', 'Suno API 超时毫秒', current_timestamp
where not exists (select 1 from sys_config where cfg_key = 'suno.api.timeout_ms');
