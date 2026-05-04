-- Reset previous demo/business data and seed a fresh dataset for current UI + flow testing.

delete from task_submit_proof;
delete from task_comment;
delete from task_order;
delete from task_attach;
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

-- Keep real user accounts; remove only historical seed/dev users.
delete from user_account where open_id like 'seed-%' or open_id like 'dev-openid-%';

-- Fresh seed users.
insert into user_account (open_id, nickname, avatar, status, credit_score, gender, signature, created_at, updated_at)
select 'seed-publisher-2026', '任务发布官', 'https://i.pravatar.cc/240?img=11', 'ACTIVE', 100, 'UNKNOWN', '官方任务发布账号', current_timestamp, current_timestamp;
insert into user_account (open_id, nickname, avatar, status, credit_score, gender, signature, created_at, updated_at)
select 'seed-user-01', '阿青', 'https://i.pravatar.cc/240?img=12', 'ACTIVE', 100, 'MALE', '接单中', current_timestamp, current_timestamp;
insert into user_account (open_id, nickname, avatar, status, credit_score, gender, signature, created_at, updated_at)
select 'seed-user-02', '小雨', 'https://i.pravatar.cc/240?img=13', 'ACTIVE', 100, 'FEMALE', '热爱分享', current_timestamp, current_timestamp;
insert into user_account (open_id, nickname, avatar, status, credit_score, gender, signature, created_at, updated_at)
select 'seed-user-03', '木木', 'https://i.pravatar.cc/240?img=14', 'ACTIVE', 99, 'MALE', '风景控', current_timestamp, current_timestamp;
insert into user_account (open_id, nickname, avatar, status, credit_score, gender, signature, created_at, updated_at)
select 'seed-user-04', '喵喵', 'https://i.pravatar.cc/240?img=15', 'ACTIVE', 100, 'FEMALE', '宠物玩家', current_timestamp, current_timestamp;
insert into user_account (open_id, nickname, avatar, status, credit_score, gender, signature, created_at, updated_at)
select 'seed-user-05', '星野', 'https://i.pravatar.cc/240?img=16', 'ACTIVE', 98, 'UNKNOWN', '游戏爱好者', current_timestamp, current_timestamp;
insert into user_account (open_id, nickname, avatar, status, credit_score, gender, signature, created_at, updated_at)
select 'seed-user-06', '知秋', 'https://i.pravatar.cc/240?img=17', 'ACTIVE', 100, 'MALE', '知识搬运工', current_timestamp, current_timestamp;
insert into user_account (open_id, nickname, avatar, status, credit_score, gender, signature, created_at, updated_at)
select 'seed-user-07', '南栀', 'https://i.pravatar.cc/240?img=18', 'ACTIVE', 100, 'FEMALE', '日常记录者', current_timestamp, current_timestamp;
insert into user_account (open_id, nickname, avatar, status, credit_score, gender, signature, created_at, updated_at)
select 'seed-user-08', '阿北', 'https://i.pravatar.cc/240?img=19', 'ACTIVE', 97, 'UNKNOWN', '科技迷', current_timestamp, current_timestamp;

-- Wallet initialization for fresh seed users.
insert into wallet_account (user_id, balance, frozen_amount, total_income, created_at, updated_at)
select u.id,
       cast((120 + mod(u.id * 37, 500)) as decimal(12,2)),
       cast((10 + mod(u.id * 11, 50)) as decimal(12,2)),
       cast((200 + mod(u.id * 53, 1000)) as decimal(12,2)),
       current_timestamp, current_timestamp
from user_account u
where u.open_id like 'seed-%';

-- Fresh task list data (100 tasks, majority published for paging).
insert into task_publish (
  task_no, publisher_id, title, content, category, location_text, amount, total_slots, accepted_slots,
  deadline_at, proof_requirements, status, reject_reason, created_at, updated_at
)
select
  concat('NT', lpad(cast(n as varchar), 6, '0')),
  (select id from user_account where open_id = 'seed-publisher-2026'),
  concat('同城任务 #', cast(n as varchar)),
  concat('第', cast(n as varchar), '号测试任务，支持文字或图片提交，按要求完成即可。'),
  case mod(n, 10)
    when 0 then '跑腿'
    when 1 then '探店'
    when 2 then '拍照'
    when 3 then '问卷'
    when 4 then '地推'
    when 5 then '资料整理'
    when 6 then '游戏'
    when 7 then '美食'
    when 8 then '科技'
    else '日常'
  end,
  concat('耒阳区域-', cast(mod(n, 12) + 1 as varchar)),
  cast((5 + mod(n, 45)) as decimal(10,2)),
  20 + mod(n, 20),
  mod(n, 8),
  dateadd('DAY', 2 + mod(n, 20), current_timestamp),
  case when mod(n, 3) = 0 then '可纯文字提交，图片可选' else '建议上传1-3张图片或文字说明' end,
  case when mod(n, 11) = 0 then 'PENDING_AUDIT' else 'PUBLISHED' end,
  null,
  dateadd('MINUTE', -n, current_timestamp),
  dateadd('MINUTE', -n, current_timestamp)
from system_range(1, 100) s(n);

-- Fresh order dataset with mixed statuses.
insert into task_order (
  order_no, task_id, accept_user_id, order_status, accept_time, submit_time, audit_reason, settled_time, created_at, updated_at
)
select
  concat('NO', lpad(cast(n as varchar), 8, '0')),
  (select id from task_publish where task_no = concat('NT', lpad(cast(n as varchar), 6, '0'))),
  case mod(n, 8)
    when 0 then (select id from user_account where open_id = 'seed-user-01')
    when 1 then (select id from user_account where open_id = 'seed-user-02')
    when 2 then (select id from user_account where open_id = 'seed-user-03')
    when 3 then (select id from user_account where open_id = 'seed-user-04')
    when 4 then (select id from user_account where open_id = 'seed-user-05')
    when 5 then (select id from user_account where open_id = 'seed-user-06')
    when 6 then (select id from user_account where open_id = 'seed-user-07')
    else (select id from user_account where open_id = 'seed-user-08')
  end,
  case mod(n, 7)
    when 0 then 'ACCEPTED'
    when 1 then 'SUBMITTED'
    when 2 then 'APPROVED'
    when 3 then 'REJECTED_RESUBMIT'
    when 4 then 'REJECTED_CLOSE'
    when 5 then 'SETTLED'
    else 'TIMEOUT'
  end,
  dateadd('HOUR', -6 - n, current_timestamp),
  case
    when mod(n, 7) in (0, 6) then null
    else dateadd('HOUR', -5 - n, current_timestamp)
  end,
  case
    when mod(n, 7) = 3 then '图片不清晰，请重提'
    when mod(n, 7) = 4 then '任务不符合要求，已关闭'
    else null
  end,
  case when mod(n, 7) = 5 then dateadd('HOUR', -2 - n, current_timestamp) else null end,
  dateadd('HOUR', -6 - n, current_timestamp),
  dateadd('HOUR', -4 - n, current_timestamp)
from system_range(1, 40) s(n);

insert into task_submit_proof (order_id, proof_type, proof_url, remark, created_at)
select o.id,
       'IMAGE',
       concat('https://picsum.photos/seed/newproof', cast(o.id as varchar), '/900/600'),
       '现场图片凭证',
       dateadd('MINUTE', 1, o.accept_time)
from task_order o
where o.submit_time is not null;

insert into task_submit_proof (order_id, proof_type, proof_url, remark, created_at)
select o.id,
       'TEXT',
       'TEXT_CONTENT',
       concat('文字说明：订单 ', o.order_no, ' 已按要求处理。'),
       dateadd('MINUTE', 2, o.accept_time)
from task_order o
where o.submit_time is not null;

update task_publish t
set accepted_slots = (
  select count(*) from task_order o where o.task_id = t.id
)
where t.task_no like 'NT%';

-- Fresh plaza data (160 posts).
insert into plaza_post (
  author_id, content, gender, category, like_count, comment_count, created_at, updated_at
)
select
  case mod(n, 8)
    when 0 then (select id from user_account where open_id = 'seed-user-01')
    when 1 then (select id from user_account where open_id = 'seed-user-02')
    when 2 then (select id from user_account where open_id = 'seed-user-03')
    when 3 then (select id from user_account where open_id = 'seed-user-04')
    when 4 then (select id from user_account where open_id = 'seed-user-05')
    when 5 then (select id from user_account where open_id = 'seed-user-06')
    when 6 then (select id from user_account where open_id = 'seed-user-07')
    else (select id from user_account where open_id = 'seed-user-08')
  end,
  concat('广场新内容 #', cast(n as varchar), '，用于热门/最新/关注和分类联调。'),
  case mod(n, 3) when 0 then 'MALE' when 1 then 'FEMALE' else 'UNKNOWN' end,
  case mod(n, 11)
    when 0 then 'MALE'
    when 1 then 'FEMALE'
    when 2 then 'SCENERY'
    when 3 then 'PET'
    when 4 then 'FUNNY'
    when 5 then 'ART'
    when 6 then 'GAME'
    when 7 then 'TECH'
    when 8 then 'KNOWLEDGE'
    when 9 then 'DAILY'
    else 'OTHER'
  end,
  0, 0,
  dateadd('MINUTE', -n, current_timestamp),
  dateadd('MINUTE', -n, current_timestamp)
from system_range(1, 160) s(n);

insert into plaza_post_image (post_id, image_url, sort_no, created_at)
select p.id,
       concat('https://picsum.photos/seed/newplaza', cast(p.id as varchar), '/1000/700'),
       0,
       current_timestamp
from plaza_post p
where mod(p.id, 3) <> 0;

insert into plaza_post_like (post_id, user_id, created_at, updated_at)
select p.id,
       (select id from user_account where open_id = 'seed-user-01'),
       current_timestamp, current_timestamp
from plaza_post p
where p.author_id <> (select id from user_account where open_id = 'seed-user-01');

insert into plaza_post_comment (post_id, user_id, content, created_at, updated_at)
select p.id,
       case mod(p.id, 4)
         when 0 then (select id from user_account where open_id = 'seed-user-02')
         when 1 then (select id from user_account where open_id = 'seed-user-03')
         when 2 then (select id from user_account where open_id = 'seed-user-04')
         else (select id from user_account where open_id = 'seed-user-05')
       end,
       concat('这是广场评论，post=', cast(p.id as varchar)),
       dateadd('MINUTE', 1, p.created_at),
       dateadd('MINUTE', 1, p.created_at)
from plaza_post p
where mod(p.id, 2) = 0;

update plaza_post p
set like_count = (select count(*) from plaza_post_like l where l.post_id = p.id),
    comment_count = (select count(*) from plaza_post_comment c where c.post_id = p.id)
where p.id in (select id from plaza_post);

-- Fresh follow relations for "关注" feed.
insert into plaza_follow (user_id, target_user_id, created_at)
values
((select id from user_account where open_id = 'seed-user-01'), (select id from user_account where open_id = 'seed-user-02'), current_timestamp),
((select id from user_account where open_id = 'seed-user-01'), (select id from user_account where open_id = 'seed-user-03'), current_timestamp),
((select id from user_account where open_id = 'seed-user-02'), (select id from user_account where open_id = 'seed-user-01'), current_timestamp);

-- Fresh withdraw records for "工资墙".
insert into withdraw_apply (apply_no, user_id, amount, channel, qr_code_url, audit_status, audit_reason, created_at, updated_at)
values
('NW260001', (select id from user_account where open_id = 'seed-user-01'), 58.80, 'WECHAT_QR', 'https://picsum.photos/seed/qr001/480/480', 'PENDING', null, dateadd('DAY', -1, current_timestamp), dateadd('DAY', -1, current_timestamp)),
('NW260002', (select id from user_account where open_id = 'seed-user-01'), 120.00, 'WECHAT_QR', 'https://picsum.photos/seed/qr002/480/480', 'PAID', null, dateadd('DAY', -3, current_timestamp), dateadd('DAY', -2, current_timestamp)),
('NW260003', (select id from user_account where open_id = 'seed-user-02'), 66.00, 'WECHAT_QR', 'https://picsum.photos/seed/qr003/480/480', 'REJECTED', '收款码不清晰', dateadd('DAY', -2, current_timestamp), dateadd('DAY', -2, current_timestamp));

insert into wallet_flow (user_id, flow_type, amount, biz_no, status, created_at)
values
((select id from user_account where open_id = 'seed-user-01'), 'WITHDRAW_APPLY_FREEZE', 58.80, 'NW260001', 'SUCCESS', dateadd('DAY', -1, current_timestamp)),
((select id from user_account where open_id = 'seed-user-01'), 'WITHDRAW_PAID', 120.00, 'NW260002', 'SUCCESS', dateadd('DAY', -2, current_timestamp)),
((select id from user_account where open_id = 'seed-user-02'), 'WITHDRAW_REJECT_UNFREEZE', 66.00, 'NW260003', 'SUCCESS', dateadd('DAY', -2, current_timestamp));
