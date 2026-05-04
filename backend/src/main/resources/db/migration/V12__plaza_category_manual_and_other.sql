insert into plaza_category (code, name, keywords, status, sort_no, created_at, updated_at)
select 'MALE', '男生', '男生,男', 'ACTIVE', 5, current_timestamp, current_timestamp
where not exists (select 1 from plaza_category where code = 'MALE');

insert into plaza_category (code, name, keywords, status, sort_no, created_at, updated_at)
select 'FEMALE', '女生', '女生,女', 'ACTIVE', 6, current_timestamp, current_timestamp
where not exists (select 1 from plaza_category where code = 'FEMALE');

insert into plaza_category (code, name, keywords, status, sort_no, created_at, updated_at)
select 'FOOD', '美食', '美食,吃,餐厅,探店,奶茶,咖啡', 'ACTIVE', 35, current_timestamp, current_timestamp
where not exists (select 1 from plaza_category where code = 'FOOD');

insert into plaza_category (code, name, keywords, status, sort_no, created_at, updated_at)
select 'OTHER', '其他', '其他,未分类', 'ACTIVE', 999, current_timestamp, current_timestamp
where not exists (select 1 from plaza_category where code = 'OTHER');
