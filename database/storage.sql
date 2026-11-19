-- Apply once using Supabase SQL editor. Private objects are served via short-lived URLs from Spring.
insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
values ('civic-evidence','civic-evidence',false,3145728,array['image/jpeg','image/png'])
on conflict (id) do nothing;
-- No client policies: only the backend service key can upload/sign objects.
