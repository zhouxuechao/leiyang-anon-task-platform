insert into sys_config (cfg_key, cfg_value, remark, updated_at)
select 'ui.nav_hot_tabs', 'plaza,tasks,music', '用户端左侧导航火热标记，逗号分隔：plaza,tasks,music,me', current_timestamp
where not exists (select 1 from sys_config where cfg_key = 'ui.nav_hot_tabs');
