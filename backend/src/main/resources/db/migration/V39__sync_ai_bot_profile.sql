update user_account u
set
  nickname = (
    select p.name || ' AI'
    from plaza_ai_provider p
    where lower(p.code) = substring(u.open_id, 8)
  ),
  avatar = coalesce((
    select p.logo_url
    from plaza_ai_provider p
    where lower(p.code) = substring(u.open_id, 8)
  ), avatar)
where u.open_id like 'ai-bot:%'
  and exists (
    select 1
    from plaza_ai_provider p
    where lower(p.code) = substring(u.open_id, 8)
  );
