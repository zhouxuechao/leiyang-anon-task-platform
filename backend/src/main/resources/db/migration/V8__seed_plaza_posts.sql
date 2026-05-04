insert into plaza_post (author_id, content, gender, like_count, comment_count, created_at, updated_at)
select u.id, '今天在商圈拍了3家门店，客流挺大，晚上补图。', 'MALE', 12, 3, current_timestamp, current_timestamp
from user_account u
where u.open_id = 'seed-taker-001'
  and not exists (select 1 from plaza_post p where p.content = '今天在商圈拍了3家门店，客流挺大，晚上补图。');

insert into plaza_post (author_id, content, gender, like_count, comment_count, created_at, updated_at)
select u.id, '刚做完问卷任务，效率不错，大家可以冲。', 'FEMALE', 18, 6, current_timestamp, current_timestamp
from user_account u
where u.open_id = 'seed-taker-002'
  and not exists (select 1 from plaza_post p where p.content = '刚做完问卷任务，效率不错，大家可以冲。');

insert into plaza_post (author_id, content, gender, like_count, comment_count, created_at, updated_at)
select u.id, '晚高峰拍排队长度，注意选好机位，别挡路。', 'MALE', 9, 2, current_timestamp, current_timestamp
from user_account u
where u.open_id = 'seed-taker-003'
  and not exists (select 1 from plaza_post p where p.content = '晚高峰拍排队长度，注意选好机位，别挡路。');

insert into plaza_post_image (post_id, image_url, sort_no, created_at)
select p.id, 'https://picsum.photos/seed/plaza001/800/600', 0, current_timestamp
from plaza_post p
where p.content = '今天在商圈拍了3家门店，客流挺大，晚上补图。'
  and not exists (select 1 from plaza_post_image i where i.post_id = p.id and i.sort_no = 0);

insert into plaza_post_image (post_id, image_url, sort_no, created_at)
select p.id, 'https://picsum.photos/seed/plaza002/800/600', 1, current_timestamp
from plaza_post p
where p.content = '今天在商圈拍了3家门店，客流挺大，晚上补图。'
  and not exists (select 1 from plaza_post_image i where i.post_id = p.id and i.sort_no = 1);

insert into plaza_post_image (post_id, image_url, sort_no, created_at)
select p.id, 'https://picsum.photos/seed/plaza003/800/600', 0, current_timestamp
from plaza_post p
where p.content = '刚做完问卷任务，效率不错，大家可以冲。'
  and not exists (select 1 from plaza_post_image i where i.post_id = p.id and i.sort_no = 0);

insert into plaza_post_image (post_id, image_url, sort_no, created_at)
select p.id, 'https://picsum.photos/seed/plaza004/800/600', 0, current_timestamp
from plaza_post p
where p.content = '晚高峰拍排队长度，注意选好机位，别挡路。'
  and not exists (select 1 from plaza_post_image i where i.post_id = p.id and i.sort_no = 0);

insert into plaza_follow (user_id, target_user_id, created_at)
select a.id, b.id, current_timestamp
from user_account a, user_account b
where a.open_id = 'seed-taker-001' and b.open_id = 'seed-taker-002'
  and not exists (
    select 1 from plaza_follow f where f.user_id = a.id and f.target_user_id = b.id
  );

