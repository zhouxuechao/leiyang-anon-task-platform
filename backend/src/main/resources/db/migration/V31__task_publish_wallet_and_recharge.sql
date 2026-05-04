alter table task_publish add column if not exists frozen_amount decimal(12,2) not null default 0;
alter table task_publish add column if not exists publish_fee_settled boolean not null default false;

insert into sys_config (cfg_key, cfg_value, remark, updated_at)
select 'recharge.wechat.qr_url', '', '平台微信收款码图片URL', current_timestamp
where not exists (select 1 from sys_config where cfg_key = 'recharge.wechat.qr_url');

insert into sys_config (cfg_key, cfg_value, remark, updated_at)
select 'recharge.wechat.name', '叼瓜赖圈', '平台收款方名称', current_timestamp
where not exists (select 1 from sys_config where cfg_key = 'recharge.wechat.name');
