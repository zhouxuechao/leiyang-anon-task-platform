alter table ai_music_job alter column title varchar(100);
alter table ai_music_job alter column style varchar(1000);

update sys_config
set cfg_value = 'V5_5',
    remark = 'Suno API 模型',
    updated_at = current_timestamp
where cfg_key = 'suno.api.model'
  and cfg_value in ('', 'V4_5', 'V4_5ALL', 'V4_5PLUS', 'V5', 'V5_0');
