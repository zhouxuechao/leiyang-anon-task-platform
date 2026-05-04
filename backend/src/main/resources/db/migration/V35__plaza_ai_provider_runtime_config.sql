alter table plaza_ai_provider add column if not exists base_url varchar(255);
alter table plaza_ai_provider add column if not exists model varchar(128);
alter table plaza_ai_provider add column if not exists api_key varchar(512);
alter table plaza_ai_provider add column if not exists timeout_ms integer;
alter table plaza_ai_provider add column if not exists temperature double precision;

update plaza_ai_provider p
set base_url = (select c.cfg_value from sys_config c where c.cfg_key = 'ai.provider.' || lower(p.code) || '.base_url')
where p.base_url is null
  and exists (select 1 from sys_config c where c.cfg_key = 'ai.provider.' || lower(p.code) || '.base_url');

update plaza_ai_provider p
set model = (select c.cfg_value from sys_config c where c.cfg_key = 'ai.provider.' || lower(p.code) || '.model')
where p.model is null
  and exists (select 1 from sys_config c where c.cfg_key = 'ai.provider.' || lower(p.code) || '.model');

update plaza_ai_provider p
set api_key = (select c.cfg_value from sys_config c where c.cfg_key = 'ai.provider.' || lower(p.code) || '.api_key')
where p.api_key is null
  and exists (select 1 from sys_config c where c.cfg_key = 'ai.provider.' || lower(p.code) || '.api_key');

update plaza_ai_provider p
set timeout_ms = cast((select c.cfg_value from sys_config c where c.cfg_key = 'ai.provider.' || lower(p.code) || '.timeout_ms') as integer)
where p.timeout_ms is null
  and exists (select 1 from sys_config c where c.cfg_key = 'ai.provider.' || lower(p.code) || '.timeout_ms');

update plaza_ai_provider p
set temperature = cast((select c.cfg_value from sys_config c where c.cfg_key = 'ai.provider.' || lower(p.code) || '.temperature') as double precision)
where p.temperature is null
  and exists (select 1 from sys_config c where c.cfg_key = 'ai.provider.' || lower(p.code) || '.temperature');

update plaza_ai_provider p
set status = 'INACTIVE'
where coalesce((select c.cfg_value from sys_config c where c.cfg_key = 'ai.provider.' || lower(p.code) || '.enabled'), 'true') = 'false';
