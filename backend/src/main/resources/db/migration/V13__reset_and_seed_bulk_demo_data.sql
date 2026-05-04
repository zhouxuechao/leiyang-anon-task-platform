-- Reset business demo data (keep config/meta tables), then seed bulk plaza/tasks data for pagination testing.

delete from task_submit_proof;
delete from task_comment;
delete from task_order;
delete from task_publish;

delete from plaza_post_comment;
delete from plaza_post_like;
delete from plaza_post_image;
delete from plaza_follow;
delete from plaza_post;

delete from user_message;
delete from report_record;
delete from withdraw_apply;
delete from wallet_flow;
delete from wallet_account;
delete from user_auth;

-- Ensure seed users exist
insert into user_account (open_id, nickname, avatar, status, credit_score, gender, created_at, updated_at)
select 'seed-publisher-auto', '发布者A', null, 'ACTIVE', 100, 'MALE', current_timestamp, current_timestamp
where not exists (select 1 from user_account where open_id = 'seed-publisher-auto');

insert into user_account (open_id, nickname, avatar, status, credit_score, gender, created_at, updated_at)
select 'seed-u-01', '测试用户01', null, 'ACTIVE', 100, 'MALE', current_timestamp, current_timestamp
where not exists (select 1 from user_account where open_id = 'seed-u-01');

insert into user_account (open_id, nickname, avatar, status, credit_score, gender, created_at, updated_at)
select 'seed-u-02', '测试用户02', null, 'ACTIVE', 100, 'FEMALE', current_timestamp, current_timestamp
where not exists (select 1 from user_account where open_id = 'seed-u-02');

insert into user_account (open_id, nickname, avatar, status, credit_score, gender, created_at, updated_at)
select 'seed-u-03', '测试用户03', null, 'ACTIVE', 100, 'MALE', current_timestamp, current_timestamp
where not exists (select 1 from user_account where open_id = 'seed-u-03');

insert into user_account (open_id, nickname, avatar, status, credit_score, gender, created_at, updated_at)
select 'seed-u-04', '测试用户04', null, 'ACTIVE', 100, 'FEMALE', current_timestamp, current_timestamp
where not exists (select 1 from user_account where open_id = 'seed-u-04');

insert into user_account (open_id, nickname, avatar, status, credit_score, gender, created_at, updated_at)
select 'seed-u-05', '测试用户05', null, 'ACTIVE', 100, 'UNKNOWN', current_timestamp, current_timestamp
where not exists (select 1 from user_account where open_id = 'seed-u-05');

-- Seed 60 published tasks
insert into task_publish (
  task_no, publisher_id, title, content, category, location_text, amount, total_slots, accepted_slots,
  deadline_at, proof_requirements, status, reject_reason, created_at, updated_at
)
select
  concat('TP', lpad(cast(n as varchar), 6, '0')),
  (select id from user_account where open_id = 'seed-publisher-auto'),
  concat('测试任务 #', cast(n as varchar)),
  concat('这是第', cast(n as varchar), '条测试任务，请上传对应凭证完成。'),
  case mod(n, 8)
    when 0 then '跑腿'
    when 1 then '拍照'
    when 2 then '探店'
    when 3 then '问卷'
    when 4 then '游戏'
    when 5 then '美食'
    when 6 then '风景'
    else '其他'
  end,
  concat('测试地点-', cast(mod(n, 12) + 1 as varchar)),
  cast((3 + mod(n, 20)) as decimal(10, 2)),
  20,
  mod(n, 5),
  dateadd('DAY', 20 + mod(n, 20), current_timestamp),
  '上传1-3张图片',
  'PUBLISHED',
  null,
  dateadd('MINUTE', -n, current_timestamp),
  dateadd('MINUTE', -n, current_timestamp)
from system_range(1, 60) s(n);

-- Seed 80 plaza posts
insert into plaza_post (
  author_id, content, gender, category, like_count, comment_count, created_at, updated_at
)
select
  case mod(n, 5)
    when 0 then (select id from user_account where open_id = 'seed-u-01')
    when 1 then (select id from user_account where open_id = 'seed-u-02')
    when 2 then (select id from user_account where open_id = 'seed-u-03')
    when 3 then (select id from user_account where open_id = 'seed-u-04')
    else (select id from user_account where open_id = 'seed-u-05')
  end,
  concat('测试广场内容 #', cast(n as varchar), '，用于分页与下拉加载联调。'),
  case mod(n, 3)
    when 0 then 'MALE'
    when 1 then 'FEMALE'
    else 'UNKNOWN'
  end,
  case mod(n, 10)
    when 0 then 'MALE'
    when 1 then 'FEMALE'
    when 2 then 'FOOD'
    when 3 then 'SCENERY'
    when 4 then 'PET'
    when 5 then 'FUNNY'
    when 6 then 'ART'
    when 7 then 'GAME'
    when 8 then 'TECH'
    else 'OTHER'
  end,
  mod(n, 9),
  mod(n, 5),
  dateadd('MINUTE', -n, current_timestamp),
  dateadd('MINUTE', -n, current_timestamp)
from system_range(1, 80) s(n);

-- Attach one image per seeded plaza post
insert into plaza_post_image (post_id, image_url, sort_no, created_at)
select
  p.id,
  concat('https://picsum.photos/seed/plaza', cast(p.id as varchar), '/800/600'),
  0,
  current_timestamp
from plaza_post p
where p.content like '测试广场内容 #%';
