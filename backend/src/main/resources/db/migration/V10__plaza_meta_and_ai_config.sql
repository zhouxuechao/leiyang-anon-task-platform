create table if not exists plaza_category (
  id bigint primary key auto_increment,
  code varchar(64) not null unique,
  name varchar(64) not null unique,
  keywords varchar(512) null,
  status varchar(16) not null,
  sort_no int not null default 0,
  created_at timestamp not null,
  updated_at timestamp not null
);

create table if not exists plaza_ai_provider (
  id bigint primary key auto_increment,
  code varchar(64) not null unique,
  name varchar(64) not null,
  abbr varchar(16) not null,
  logo_text varchar(16) null,
  status varchar(16) not null,
  sort_no int not null default 0,
  created_at timestamp not null,
  updated_at timestamp not null
);

create table if not exists plaza_sort_option (
  id bigint primary key auto_increment,
  code varchar(32) not null unique,
  name varchar(32) not null,
  status varchar(16) not null,
  sort_no int not null default 0,
  created_at timestamp not null,
  updated_at timestamp not null
);

insert into plaza_category (code, name, keywords, status, sort_no, created_at, updated_at)
select 'SCENERY', '风景', '风景,天空,旅行,海边,日落,打卡,山', 'ACTIVE', 10, current_timestamp, current_timestamp
where not exists (select 1 from plaza_category where code = 'SCENERY');

insert into plaza_category (code, name, keywords, status, sort_no, created_at, updated_at)
select 'PET', '宠物', '宠物,猫,狗,撸猫,萌宠', 'ACTIVE', 20, current_timestamp, current_timestamp
where not exists (select 1 from plaza_category where code = 'PET');

insert into plaza_category (code, name, keywords, status, sort_no, created_at, updated_at)
select 'FUNNY', '搞笑', '搞笑,整活,段子,爆笑,梗', 'ACTIVE', 30, current_timestamp, current_timestamp
where not exists (select 1 from plaza_category where code = 'FUNNY');

insert into plaza_category (code, name, keywords, status, sort_no, created_at, updated_at)
select 'ART', '艺术', '艺术,绘画,摄影,创作,设计', 'ACTIVE', 40, current_timestamp, current_timestamp
where not exists (select 1 from plaza_category where code = 'ART');

insert into plaza_category (code, name, keywords, status, sort_no, created_at, updated_at)
select 'GAME', '游戏', '游戏,开黑,上分,王者,吃鸡', 'ACTIVE', 50, current_timestamp, current_timestamp
where not exists (select 1 from plaza_category where code = 'GAME');

insert into plaza_category (code, name, keywords, status, sort_no, created_at, updated_at)
select 'TECH', '科技', '科技,AI,编程,数码,模型,算法', 'ACTIVE', 60, current_timestamp, current_timestamp
where not exists (select 1 from plaza_category where code = 'TECH');

insert into plaza_category (code, name, keywords, status, sort_no, created_at, updated_at)
select 'KNOWLEDGE', '知识', '知识,科普,教程,学习,经验', 'ACTIVE', 70, current_timestamp, current_timestamp
where not exists (select 1 from plaza_category where code = 'KNOWLEDGE');

insert into plaza_category (code, name, keywords, status, sort_no, created_at, updated_at)
select 'DAILY', '日常', '日常,生活,记录,分享', 'ACTIVE', 80, current_timestamp, current_timestamp
where not exists (select 1 from plaza_category where code = 'DAILY');

insert into plaza_ai_provider (code, name, abbr, logo_text, status, sort_no, created_at, updated_at)
select 'doubao', '豆包', 'DB', '豆', 'ACTIVE', 10, current_timestamp, current_timestamp
where not exists (select 1 from plaza_ai_provider where code = 'doubao');

insert into plaza_ai_provider (code, name, abbr, logo_text, status, sort_no, created_at, updated_at)
select 'qwen', '千问', 'QW', '千', 'ACTIVE', 20, current_timestamp, current_timestamp
where not exists (select 1 from plaza_ai_provider where code = 'qwen');

insert into plaza_ai_provider (code, name, abbr, logo_text, status, sort_no, created_at, updated_at)
select 'deepseek', 'DeepSeek', 'DS', 'D', 'ACTIVE', 30, current_timestamp, current_timestamp
where not exists (select 1 from plaza_ai_provider where code = 'deepseek');

insert into plaza_ai_provider (code, name, abbr, logo_text, status, sort_no, created_at, updated_at)
select 'kimi', 'Kimi', 'KM', 'K', 'ACTIVE', 40, current_timestamp, current_timestamp
where not exists (select 1 from plaza_ai_provider where code = 'kimi');

insert into plaza_ai_provider (code, name, abbr, logo_text, status, sort_no, created_at, updated_at)
select 'gpt', 'GPT', 'GPT', 'G', 'ACTIVE', 50, current_timestamp, current_timestamp
where not exists (select 1 from plaza_ai_provider where code = 'gpt');

insert into plaza_sort_option (code, name, status, sort_no, created_at, updated_at)
select 'HOT', '热门', 'ACTIVE', 10, current_timestamp, current_timestamp
where not exists (select 1 from plaza_sort_option where code = 'HOT');

insert into plaza_sort_option (code, name, status, sort_no, created_at, updated_at)
select 'LATEST', '最新', 'ACTIVE', 20, current_timestamp, current_timestamp
where not exists (select 1 from plaza_sort_option where code = 'LATEST');

insert into plaza_sort_option (code, name, status, sort_no, created_at, updated_at)
select 'FOLLOW', '关注', 'ACTIVE', 30, current_timestamp, current_timestamp
where not exists (select 1 from plaza_sort_option where code = 'FOLLOW');
