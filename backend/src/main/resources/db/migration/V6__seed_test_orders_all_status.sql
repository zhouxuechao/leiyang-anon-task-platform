-- Seed richer demo data for MP/Admin testing:
-- - more published tasks
-- - orders covering all 7 order statuses
-- - submission proofs for submitted-like statuses

insert into user_account (open_id, nickname, avatar, status, credit_score, created_at, updated_at)
select 'seed-publisher-001', '测试发布者', null, 'ACTIVE', 100, current_timestamp, current_timestamp
where not exists (select 1 from user_account where open_id = 'seed-publisher-001');

insert into user_account (open_id, nickname, avatar, status, credit_score, created_at, updated_at)
select 'seed-taker-001', '接单员A', null, 'ACTIVE', 100, current_timestamp, current_timestamp
where not exists (select 1 from user_account where open_id = 'seed-taker-001');

insert into user_account (open_id, nickname, avatar, status, credit_score, created_at, updated_at)
select 'seed-taker-002', '接单员B', null, 'ACTIVE', 98, current_timestamp, current_timestamp
where not exists (select 1 from user_account where open_id = 'seed-taker-002');

insert into user_account (open_id, nickname, avatar, status, credit_score, created_at, updated_at)
select 'seed-taker-003', '接单员C', null, 'ACTIVE', 96, current_timestamp, current_timestamp
where not exists (select 1 from user_account where open_id = 'seed-taker-003');

insert into user_account (open_id, nickname, avatar, status, credit_score, created_at, updated_at)
select 'seed-taker-004', '接单员D', null, 'ACTIVE', 95, current_timestamp, current_timestamp
where not exists (select 1 from user_account where open_id = 'seed-taker-004');

insert into user_account (open_id, nickname, avatar, status, credit_score, created_at, updated_at)
select 'seed-taker-005', '接单员E', null, 'ACTIVE', 94, current_timestamp, current_timestamp
where not exists (select 1 from user_account where open_id = 'seed-taker-005');

insert into user_account (open_id, nickname, avatar, status, credit_score, created_at, updated_at)
select 'seed-taker-006', '接单员F', null, 'ACTIVE', 93, current_timestamp, current_timestamp
where not exists (select 1 from user_account where open_id = 'seed-taker-006');

insert into user_account (open_id, nickname, avatar, status, credit_score, created_at, updated_at)
select 'seed-taker-007', '接单员G', null, 'ACTIVE', 92, current_timestamp, current_timestamp
where not exists (select 1 from user_account where open_id = 'seed-taker-007');

insert into user_auth (user_id, real_name, id_no_enc, mobile, pay_account, status, created_at, updated_at)
select u.id, u.nickname, 'seed', '13800000000', 'wx-seed', 'VERIFIED', current_timestamp, current_timestamp
from user_account u
where u.open_id like 'seed-%'
  and not exists (select 1 from user_auth a where a.user_id = u.id);

insert into wallet_account (user_id, balance, frozen_amount, total_income, created_at, updated_at)
select u.id, 1000.00, 0.00, 0.00, current_timestamp, current_timestamp
from user_account u
where u.open_id like 'seed-%'
  and not exists (select 1 from wallet_account w where w.user_id = u.id);

insert into task_publish
  (task_no, publisher_id, title, content, category, location_text, amount, total_slots, accepted_slots, deadline_at, proof_requirements, status, reject_reason, created_at, updated_at)
select 'TST260422001', p.id, '拍店招并上传定位截图', '到指定商圈拍摄店招并附上定位截图。', '拍照', '耒阳中心广场', 6.00, 5, 1, current_timestamp, '1张店招 + 1张定位', 'PUBLISHED', null, current_timestamp, current_timestamp
from user_account p where p.open_id = 'seed-publisher-001'
and not exists (select 1 from task_publish t where t.task_no = 'TST260422001');

insert into task_publish
  (task_no, publisher_id, title, content, category, location_text, amount, total_slots, accepted_slots, deadline_at, proof_requirements, status, reject_reason, created_at, updated_at)
select 'TST260422002', p.id, '商圈问卷采样', '现场邀请路人完成3份问卷。', '问卷', '耒阳步行街', 8.00, 5, 1, current_timestamp, '问卷提交截图', 'PUBLISHED', null, current_timestamp, current_timestamp
from user_account p where p.open_id = 'seed-publisher-001'
and not exists (select 1 from task_publish t where t.task_no = 'TST260422002');

insert into task_publish
  (task_no, publisher_id, title, content, category, location_text, amount, total_slots, accepted_slots, deadline_at, proof_requirements, status, reject_reason, created_at, updated_at)
select 'TST260422003', p.id, '超市价签采集', '采集5个商品价签与货架图。', '拍照', '耒阳大润发', 9.00, 6, 1, current_timestamp, '5张价签图', 'PUBLISHED', null, current_timestamp, current_timestamp
from user_account p where p.open_id = 'seed-publisher-001'
and not exists (select 1 from task_publish t where t.task_no = 'TST260422003');

insert into task_publish
  (task_no, publisher_id, title, content, category, location_text, amount, total_slots, accepted_slots, deadline_at, proof_requirements, status, reject_reason, created_at, updated_at)
select 'TST260422004', p.id, '门店客流拍摄', '高峰时段拍摄门店客流短视频。', '探店', '耒阳万达', 10.00, 6, 1, current_timestamp, '30秒视频', 'PUBLISHED', null, current_timestamp, current_timestamp
from user_account p where p.open_id = 'seed-publisher-001'
and not exists (select 1 from task_publish t where t.task_no = 'TST260422004');

insert into task_publish
  (task_no, publisher_id, title, content, category, location_text, amount, total_slots, accepted_slots, deadline_at, proof_requirements, status, reject_reason, created_at, updated_at)
select 'TST260422005', p.id, '外卖门店核验', '核验门店营业状态和出餐速度。', '跑腿', '耒阳金星路', 11.00, 6, 1, current_timestamp, '门头+时间截图', 'PUBLISHED', null, current_timestamp, current_timestamp
from user_account p where p.open_id = 'seed-publisher-001'
and not exists (select 1 from task_publish t where t.task_no = 'TST260422005');

insert into task_publish
  (task_no, publisher_id, title, content, category, location_text, amount, total_slots, accepted_slots, deadline_at, proof_requirements, status, reject_reason, created_at, updated_at)
select 'TST260422006', p.id, '商圈人流计数', '指定路口5分钟人流计数。', '统计', '耒阳五一广场', 12.00, 6, 1, current_timestamp, '计数表截图', 'PUBLISHED', null, current_timestamp, current_timestamp
from user_account p where p.open_id = 'seed-publisher-001'
and not exists (select 1 from task_publish t where t.task_no = 'TST260422006');

insert into task_publish
  (task_no, publisher_id, title, content, category, location_text, amount, total_slots, accepted_slots, deadline_at, proof_requirements, status, reject_reason, created_at, updated_at)
select 'TST260422007', p.id, '门店排队长度记录', '每10分钟记录一次排队长度。', '统计', '耒阳蔡伦路', 10.00, 5, 1, current_timestamp, '3次记录截图', 'PUBLISHED', null, current_timestamp, current_timestamp
from user_account p where p.open_id = 'seed-publisher-001'
and not exists (select 1 from task_publish t where t.task_no = 'TST260422007');

insert into task_publish
  (task_no, publisher_id, title, content, category, location_text, amount, total_slots, accepted_slots, deadline_at, proof_requirements, status, reject_reason, created_at, updated_at)
select 'TST260422008', p.id, '促销物料拍摄', '拍摄海报、价签、堆头。', '拍照', '耒阳神农路', 7.50, 6, 1, current_timestamp, '3张物料图', 'PUBLISHED', null, current_timestamp, current_timestamp
from user_account p where p.open_id = 'seed-publisher-001'
and not exists (select 1 from task_publish t where t.task_no = 'TST260422008');

insert into task_publish
  (task_no, publisher_id, title, content, category, location_text, amount, total_slots, accepted_slots, deadline_at, proof_requirements, status, reject_reason, created_at, updated_at)
select 'TST260422009', p.id, '停车位占用记录', '记录停车场占用率。', '统计', '耒阳西湖停车场', 6.50, 6, 1, current_timestamp, '俯拍图+计数', 'PUBLISHED', null, current_timestamp, current_timestamp
from user_account p where p.open_id = 'seed-publisher-001'
and not exists (select 1 from task_publish t where t.task_no = 'TST260422009');

insert into task_publish
  (task_no, publisher_id, title, content, category, location_text, amount, total_slots, accepted_slots, deadline_at, proof_requirements, status, reject_reason, created_at, updated_at)
select 'TST260422010', p.id, '社区便民点巡检', '巡检便民点营业情况。', '跑腿', '耒阳城北社区', 9.50, 6, 1, current_timestamp, '门头+营业时间图', 'PUBLISHED', null, current_timestamp, current_timestamp
from user_account p where p.open_id = 'seed-publisher-001'
and not exists (select 1 from task_publish t where t.task_no = 'TST260422010');

insert into task_publish
  (task_no, publisher_id, title, content, category, location_text, amount, total_slots, accepted_slots, deadline_at, proof_requirements, status, reject_reason, created_at, updated_at)
select 'TST260422011', p.id, '品牌路演签到核验', '核验路演活动签到数量。', '活动', '耒阳体育馆', 13.00, 8, 1, current_timestamp, '签到表+现场图', 'PUBLISHED', null, current_timestamp, current_timestamp
from user_account p where p.open_id = 'seed-publisher-001'
and not exists (select 1 from task_publish t where t.task_no = 'TST260422011');

insert into task_publish
  (task_no, publisher_id, title, content, category, location_text, amount, total_slots, accepted_slots, deadline_at, proof_requirements, status, reject_reason, created_at, updated_at)
select 'TST260422012', p.id, '店内陈列核对', '核对陈列与标准图差异。', '探店', '耒阳商业城', 8.80, 6, 1, current_timestamp, '对比图', 'PUBLISHED', null, current_timestamp, current_timestamp
from user_account p where p.open_id = 'seed-publisher-001'
and not exists (select 1 from task_publish t where t.task_no = 'TST260422012');

insert into task_publish
  (task_no, publisher_id, title, content, category, location_text, amount, total_slots, accepted_slots, deadline_at, proof_requirements, status, reject_reason, created_at, updated_at)
select 'TST260422013', p.id, '早高峰站点观察', '记录站点排队和等候时间。', '统计', '耒阳火车站', 12.50, 8, 1, current_timestamp, '排队图+时长记录', 'PUBLISHED', null, current_timestamp, current_timestamp
from user_account p where p.open_id = 'seed-publisher-001'
and not exists (select 1 from task_publish t where t.task_no = 'TST260422013');

insert into task_publish
  (task_no, publisher_id, title, content, category, location_text, amount, total_slots, accepted_slots, deadline_at, proof_requirements, status, reject_reason, created_at, updated_at)
select 'TST260422014', p.id, '门店服务问答', '现场咨询服务流程并记录答案。', '咨询', '耒阳新城', 11.50, 8, 1, current_timestamp, '问答记录截图', 'PUBLISHED', null, current_timestamp, current_timestamp
from user_account p where p.open_id = 'seed-publisher-001'
and not exists (select 1 from task_publish t where t.task_no = 'TST260422014');

insert into task_order
  (order_no, task_id, accept_user_id, order_status, accept_time, submit_time, audit_reason, settled_time, created_at, updated_at)
select 'OTS260422001', t.id, u.id, 'ACCEPTED', current_timestamp, null, null, null, current_timestamp, current_timestamp
from task_publish t, user_account u
where t.task_no = 'TST260422001' and u.open_id = 'seed-taker-001'
  and not exists (select 1 from task_order o where o.order_no = 'OTS260422001');

insert into task_order
  (order_no, task_id, accept_user_id, order_status, accept_time, submit_time, audit_reason, settled_time, created_at, updated_at)
select 'OTS260422002', t.id, u.id, 'ACCEPTED', current_timestamp, null, null, null, current_timestamp, current_timestamp
from task_publish t, user_account u
where t.task_no = 'TST260422002' and u.open_id = 'seed-taker-002'
  and not exists (select 1 from task_order o where o.order_no = 'OTS260422002');

insert into task_order
  (order_no, task_id, accept_user_id, order_status, accept_time, submit_time, audit_reason, settled_time, created_at, updated_at)
select 'OTS260422003', t.id, u.id, 'SUBMITTED', current_timestamp, current_timestamp, null, null, current_timestamp, current_timestamp
from task_publish t, user_account u
where t.task_no = 'TST260422003' and u.open_id = 'seed-taker-003'
  and not exists (select 1 from task_order o where o.order_no = 'OTS260422003');

insert into task_order
  (order_no, task_id, accept_user_id, order_status, accept_time, submit_time, audit_reason, settled_time, created_at, updated_at)
select 'OTS260422004', t.id, u.id, 'SUBMITTED', current_timestamp, current_timestamp, null, null, current_timestamp, current_timestamp
from task_publish t, user_account u
where t.task_no = 'TST260422004' and u.open_id = 'seed-taker-004'
  and not exists (select 1 from task_order o where o.order_no = 'OTS260422004');

insert into task_order
  (order_no, task_id, accept_user_id, order_status, accept_time, submit_time, audit_reason, settled_time, created_at, updated_at)
select 'OTS260422005', t.id, u.id, 'APPROVED', current_timestamp, current_timestamp, null, null, current_timestamp, current_timestamp
from task_publish t, user_account u
where t.task_no = 'TST260422005' and u.open_id = 'seed-taker-005'
  and not exists (select 1 from task_order o where o.order_no = 'OTS260422005');

insert into task_order
  (order_no, task_id, accept_user_id, order_status, accept_time, submit_time, audit_reason, settled_time, created_at, updated_at)
select 'OTS260422006', t.id, u.id, 'APPROVED', current_timestamp, current_timestamp, null, null, current_timestamp, current_timestamp
from task_publish t, user_account u
where t.task_no = 'TST260422006' and u.open_id = 'seed-taker-006'
  and not exists (select 1 from task_order o where o.order_no = 'OTS260422006');

insert into task_order
  (order_no, task_id, accept_user_id, order_status, accept_time, submit_time, audit_reason, settled_time, created_at, updated_at)
select 'OTS260422007', t.id, u.id, 'REJECTED_RESUBMIT', current_timestamp, current_timestamp, '图片模糊，需重传', null, current_timestamp, current_timestamp
from task_publish t, user_account u
where t.task_no = 'TST260422007' and u.open_id = 'seed-taker-007'
  and not exists (select 1 from task_order o where o.order_no = 'OTS260422007');

insert into task_order
  (order_no, task_id, accept_user_id, order_status, accept_time, submit_time, audit_reason, settled_time, created_at, updated_at)
select 'OTS260422008', t.id, u.id, 'REJECTED_RESUBMIT', current_timestamp, current_timestamp, '缺少定位截图', null, current_timestamp, current_timestamp
from task_publish t, user_account u
where t.task_no = 'TST260422008' and u.open_id = 'seed-taker-001'
  and not exists (select 1 from task_order o where o.order_no = 'OTS260422008');

insert into task_order
  (order_no, task_id, accept_user_id, order_status, accept_time, submit_time, audit_reason, settled_time, created_at, updated_at)
select 'OTS260422009', t.id, u.id, 'REJECTED_CLOSE', current_timestamp, current_timestamp, '内容不符合任务要求', null, current_timestamp, current_timestamp
from task_publish t, user_account u
where t.task_no = 'TST260422009' and u.open_id = 'seed-taker-002'
  and not exists (select 1 from task_order o where o.order_no = 'OTS260422009');

insert into task_order
  (order_no, task_id, accept_user_id, order_status, accept_time, submit_time, audit_reason, settled_time, created_at, updated_at)
select 'OTS260422010', t.id, u.id, 'REJECTED_CLOSE', current_timestamp, current_timestamp, '超过提交时限', null, current_timestamp, current_timestamp
from task_publish t, user_account u
where t.task_no = 'TST260422010' and u.open_id = 'seed-taker-003'
  and not exists (select 1 from task_order o where o.order_no = 'OTS260422010');

insert into task_order
  (order_no, task_id, accept_user_id, order_status, accept_time, submit_time, audit_reason, settled_time, created_at, updated_at)
select 'OTS260422011', t.id, u.id, 'SETTLED', current_timestamp, current_timestamp, null, current_timestamp, current_timestamp, current_timestamp
from task_publish t, user_account u
where t.task_no = 'TST260422011' and u.open_id = 'seed-taker-004'
  and not exists (select 1 from task_order o where o.order_no = 'OTS260422011');

insert into task_order
  (order_no, task_id, accept_user_id, order_status, accept_time, submit_time, audit_reason, settled_time, created_at, updated_at)
select 'OTS260422012', t.id, u.id, 'SETTLED', current_timestamp, current_timestamp, null, current_timestamp, current_timestamp, current_timestamp
from task_publish t, user_account u
where t.task_no = 'TST260422012' and u.open_id = 'seed-taker-005'
  and not exists (select 1 from task_order o where o.order_no = 'OTS260422012');

insert into task_order
  (order_no, task_id, accept_user_id, order_status, accept_time, submit_time, audit_reason, settled_time, created_at, updated_at)
select 'OTS260422013', t.id, u.id, 'TIMEOUT', current_timestamp, null, '超时未提交', null, current_timestamp, current_timestamp
from task_publish t, user_account u
where t.task_no = 'TST260422013' and u.open_id = 'seed-taker-006'
  and not exists (select 1 from task_order o where o.order_no = 'OTS260422013');

insert into task_order
  (order_no, task_id, accept_user_id, order_status, accept_time, submit_time, audit_reason, settled_time, created_at, updated_at)
select 'OTS260422014', t.id, u.id, 'TIMEOUT', current_timestamp, null, '超时未提交', null, current_timestamp, current_timestamp
from task_publish t, user_account u
where t.task_no = 'TST260422014' and u.open_id = 'seed-taker-007'
  and not exists (select 1 from task_order o where o.order_no = 'OTS260422014');

insert into task_submit_proof (order_id, proof_type, proof_url, remark, created_at)
select o.id, 'IMAGE', 'https://picsum.photos/seed/260422003/800/600', '测试凭证A', current_timestamp
from task_order o
where o.order_no = 'OTS260422003'
  and not exists (select 1 from task_submit_proof p where p.order_id = o.id);

insert into task_submit_proof (order_id, proof_type, proof_url, remark, created_at)
select o.id, 'IMAGE', 'https://picsum.photos/seed/260422004/800/600', '测试凭证B', current_timestamp
from task_order o
where o.order_no = 'OTS260422004'
  and not exists (select 1 from task_submit_proof p where p.order_id = o.id);

insert into task_submit_proof (order_id, proof_type, proof_url, remark, created_at)
select o.id, 'IMAGE', 'https://picsum.photos/seed/260422005/800/600', '审核通过样例', current_timestamp
from task_order o
where o.order_no = 'OTS260422005'
  and not exists (select 1 from task_submit_proof p where p.order_id = o.id);

insert into task_submit_proof (order_id, proof_type, proof_url, remark, created_at)
select o.id, 'IMAGE', 'https://picsum.photos/seed/260422006/800/600', '审核通过样例', current_timestamp
from task_order o
where o.order_no = 'OTS260422006'
  and not exists (select 1 from task_submit_proof p where p.order_id = o.id);

insert into task_submit_proof (order_id, proof_type, proof_url, remark, created_at)
select o.id, 'IMAGE', 'https://picsum.photos/seed/260422007/800/600', '驳回重提样例', current_timestamp
from task_order o
where o.order_no = 'OTS260422007'
  and not exists (select 1 from task_submit_proof p where p.order_id = o.id);

insert into task_submit_proof (order_id, proof_type, proof_url, remark, created_at)
select o.id, 'IMAGE', 'https://picsum.photos/seed/260422008/800/600', '驳回重提样例', current_timestamp
from task_order o
where o.order_no = 'OTS260422008'
  and not exists (select 1 from task_submit_proof p where p.order_id = o.id);

insert into task_submit_proof (order_id, proof_type, proof_url, remark, created_at)
select o.id, 'IMAGE', 'https://picsum.photos/seed/260422009/800/600', '驳回关闭样例', current_timestamp
from task_order o
where o.order_no = 'OTS260422009'
  and not exists (select 1 from task_submit_proof p where p.order_id = o.id);

insert into task_submit_proof (order_id, proof_type, proof_url, remark, created_at)
select o.id, 'IMAGE', 'https://picsum.photos/seed/260422010/800/600', '驳回关闭样例', current_timestamp
from task_order o
where o.order_no = 'OTS260422010'
  and not exists (select 1 from task_submit_proof p where p.order_id = o.id);

insert into task_submit_proof (order_id, proof_type, proof_url, remark, created_at)
select o.id, 'IMAGE', 'https://picsum.photos/seed/260422011/800/600', '已结算样例', current_timestamp
from task_order o
where o.order_no = 'OTS260422011'
  and not exists (select 1 from task_submit_proof p where p.order_id = o.id);

insert into task_submit_proof (order_id, proof_type, proof_url, remark, created_at)
select o.id, 'IMAGE', 'https://picsum.photos/seed/260422012/800/600', '已结算样例', current_timestamp
from task_order o
where o.order_no = 'OTS260422012'
  and not exists (select 1 from task_submit_proof p where p.order_id = o.id);

