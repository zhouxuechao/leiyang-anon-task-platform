-- Seed demo submitted orders for task detail completion records.

insert into task_order (
  order_no, task_id, accept_user_id, order_status, accept_time, submit_time, audit_reason, settled_time, created_at, updated_at
)
select
  'ODMOCK0001',
  (select id from task_publish where task_no = 'TP000001'),
  (select id from user_account where open_id = 'seed-u-01'),
  'SUBMITTED',
  dateadd('HOUR', -12, current_timestamp),
  dateadd('HOUR', -11, current_timestamp),
  null, null,
  dateadd('HOUR', -12, current_timestamp),
  dateadd('HOUR', -11, current_timestamp)
where not exists (select 1 from task_order where order_no = 'ODMOCK0001');

insert into task_order (
  order_no, task_id, accept_user_id, order_status, accept_time, submit_time, audit_reason, settled_time, created_at, updated_at
)
select
  'ODMOCK0002',
  (select id from task_publish where task_no = 'TP000001'),
  (select id from user_account where open_id = 'seed-u-02'),
  'SUBMITTED',
  dateadd('HOUR', -10, current_timestamp),
  dateadd('HOUR', -9, current_timestamp),
  null, null,
  dateadd('HOUR', -10, current_timestamp),
  dateadd('HOUR', -9, current_timestamp)
where not exists (select 1 from task_order where order_no = 'ODMOCK0002');

insert into task_order (
  order_no, task_id, accept_user_id, order_status, accept_time, submit_time, audit_reason, settled_time, created_at, updated_at
)
select
  'ODMOCK0003',
  (select id from task_publish where task_no = 'TP000002'),
  (select id from user_account where open_id = 'seed-u-03'),
  'SUBMITTED',
  dateadd('HOUR', -8, current_timestamp),
  dateadd('HOUR', -7, current_timestamp),
  null, null,
  dateadd('HOUR', -8, current_timestamp),
  dateadd('HOUR', -7, current_timestamp)
where not exists (select 1 from task_order where order_no = 'ODMOCK0003');

insert into task_order (
  order_no, task_id, accept_user_id, order_status, accept_time, submit_time, audit_reason, settled_time, created_at, updated_at
)
select
  'ODMOCK0004',
  (select id from task_publish where task_no = 'TP000003'),
  (select id from user_account where open_id = 'seed-u-04'),
  'SUBMITTED',
  dateadd('HOUR', -6, current_timestamp),
  dateadd('HOUR', -5, current_timestamp),
  null, null,
  dateadd('HOUR', -6, current_timestamp),
  dateadd('HOUR', -5, current_timestamp)
where not exists (select 1 from task_order where order_no = 'ODMOCK0004');

insert into task_order (
  order_no, task_id, accept_user_id, order_status, accept_time, submit_time, audit_reason, settled_time, created_at, updated_at
)
select
  'ODMOCK0005',
  (select id from task_publish where task_no = 'TP000004'),
  (select id from user_account where open_id = 'seed-u-05'),
  'SUBMITTED',
  dateadd('HOUR', -4, current_timestamp),
  dateadd('HOUR', -3, current_timestamp),
  null, null,
  dateadd('HOUR', -4, current_timestamp),
  dateadd('HOUR', -3, current_timestamp)
where not exists (select 1 from task_order where order_no = 'ODMOCK0005');

insert into task_submit_proof (order_id, proof_type, proof_url, remark, created_at)
select o.id, 'IMAGE', concat('https://picsum.photos/seed/', o.order_no, '/900/600'), null, dateadd('MINUTE', 1, o.submit_time)
from task_order o
where o.order_no in ('ODMOCK0001', 'ODMOCK0002', 'ODMOCK0003', 'ODMOCK0004', 'ODMOCK0005')
  and not exists (select 1 from task_submit_proof p where p.order_id = o.id and p.proof_type = 'IMAGE');

insert into task_submit_proof (order_id, proof_type, proof_url, remark, created_at)
select o.id, 'TEXT', 'TEXT_CONTENT', concat('完成说明：', o.order_no, '，已按任务要求提交。'), dateadd('MINUTE', 2, o.submit_time)
from task_order o
where o.order_no in ('ODMOCK0001', 'ODMOCK0002', 'ODMOCK0003', 'ODMOCK0004', 'ODMOCK0005')
  and not exists (select 1 from task_submit_proof p where p.order_id = o.id and p.proof_type = 'TEXT');

update task_publish t
set accepted_slots = (
  select count(*) from task_order o where o.task_id = t.id
)
where t.task_no in ('TP000001', 'TP000002', 'TP000003', 'TP000004');
