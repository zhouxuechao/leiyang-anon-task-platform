insert into sys_config (cfg_key, cfg_value, remark, updated_at)
select 'suno.api.key', '', 'Suno API Key', current_timestamp
where not exists (select 1 from sys_config where cfg_key = 'suno.api.key');

insert into sys_config (cfg_key, cfg_value, remark, updated_at)
select 'suno.api.callback_url', '', 'Suno API 回调地址，可为空；为空时用刷新按钮轮询结果', current_timestamp
where not exists (select 1 from sys_config where cfg_key = 'suno.api.callback_url');
