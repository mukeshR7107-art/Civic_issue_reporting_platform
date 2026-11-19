alter table civictrack.complaints add column if not exists work_started_at timestamptz;
create index if not exists attachment_resolution_cycle on civictrack.attachments(complaint_id,kind,created_at);
