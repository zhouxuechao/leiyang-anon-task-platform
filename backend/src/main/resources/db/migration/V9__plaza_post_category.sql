alter table plaza_post add column if not exists category varchar(32) default 'DAILY' not null;

update plaza_post set category = 'SCENERY' where id = 1;
update plaza_post set category = 'KNOWLEDGE' where id = 2;
update plaza_post set category = 'DAILY' where id = 3;
