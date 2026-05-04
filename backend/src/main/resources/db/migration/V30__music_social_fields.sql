alter table ai_music_job add column if not exists published boolean not null default false;
alter table ai_music_job add column if not exists published_at timestamp null;
alter table ai_music_job add column if not exists plaza_post_id bigint null;
alter table ai_music_job add column if not exists rating_total int not null default 0;
alter table ai_music_job add column if not exists rating_count int not null default 0;
alter table ai_music_job add column if not exists tip_total decimal(12,2) not null default 0;

create index if not exists idx_ai_music_job_published_created on ai_music_job(published, created_at);
