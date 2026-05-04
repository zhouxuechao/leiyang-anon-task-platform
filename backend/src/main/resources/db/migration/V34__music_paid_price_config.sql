insert into sys_config (cfg_key, cfg_value, remark, updated_at)
select 'music.paid_price', '1.20', '超出免费次数后的音乐生成单价', current_timestamp
where not exists (select 1 from sys_config where cfg_key = 'music.paid_price');
