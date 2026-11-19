create schema if not exists civictrack;
revoke all on schema civictrack from public;
create table civictrack.profiles(id uuid primary key, name varchar(255) not null, role varchar(32) not null default 'CITIZEN' check(role in ('CITIZEN','SUPERVISOR','OFFICER','ADMIN')), department varchar(255), area varchar(255));
create table civictrack.routing_rules(id uuid primary key, category varchar(255) not null, area varchar(255) not null, department varchar(255) not null, unique(category,area));
create table civictrack.complaints(
 id uuid primary key, version bigint not null default 0, tracking_number varchar(255) unique not null,
 reporter_id uuid not null references civictrack.profiles(id), title varchar(255) not null, description varchar(4000) not null,
 category varchar(255) not null, area varchar(255) not null, landmark varchar(255), latitude double precision not null check(latitude between -90 and 90), longitude double precision not null check(longitude between -180 and 180),
 department varchar(255), officer_id uuid references civictrack.profiles(id), status varchar(32) not null check(status in ('SUBMITTED','NEEDS_INFORMATION','ACKNOWLEDGED','ASSIGNED','IN_PROGRESS','RESOLUTION_PROPOSED','CLOSED','REOPENED','REJECTED')),
 priority varchar(32) not null check(priority in ('LOW','NORMAL','HIGH','CRITICAL')), urgency varchar(255), request_key uuid not null,
 created_at timestamptz not null, updated_at timestamptz not null, due_at timestamptz not null, proposed_at timestamptz, closed_at timestamptz,
 unique(reporter_id,request_key));
create table civictrack.status_events(id uuid primary key, complaint_id uuid not null references civictrack.complaints(id), actor_id uuid not null references civictrack.profiles(id), actor_name varchar(255), action varchar(255), message varchar(4000), internal boolean not null default false, created_at timestamptz not null);
create table civictrack.attachments(id uuid primary key, complaint_id uuid not null references civictrack.complaints(id), uploaded_by uuid not null references civictrack.profiles(id), object_path varchar(255) not null unique, kind varchar(32) not null check(kind in ('REPORT','RESOLUTION')), content_type varchar(255), created_at timestamptz not null);
create table civictrack.notifications(id uuid primary key, user_id uuid not null references civictrack.profiles(id), complaint_id uuid not null references civictrack.complaints(id), message varchar(4000), seen boolean not null default false, created_at timestamptz not null);
create index complaint_reporter on civictrack.complaints(reporter_id,created_at desc);
create index complaint_queue on civictrack.complaints(department,area,status,created_at desc);
create index complaint_officer on civictrack.complaints(officer_id,status);
create index complaint_deadlines on civictrack.complaints(due_at) where status not in ('CLOSED','REJECTED');
create index event_timeline on civictrack.status_events(complaint_id,created_at);
create index attachment_complaint on civictrack.attachments(complaint_id);
create index notification_user on civictrack.notifications(user_id,created_at desc);
-- No browser/Data API grants: all application data passes through Spring authorization.
alter table civictrack.profiles enable row level security;
alter table civictrack.routing_rules enable row level security;
alter table civictrack.complaints enable row level security;
alter table civictrack.status_events enable row level security;
alter table civictrack.attachments enable row level security;
alter table civictrack.notifications enable row level security;
-- JDBC uses the server database owner. Do not expose this schema through PostgREST.
insert into civictrack.routing_rules(id,category,area,department)
select gen_random_uuid(), category, area, department from (values ('Streetlight','Electrical maintenance'),('Garbage','Sanitation'),('Pothole','Roads and transport'),('Water leakage','Water supply'),('Public facility','Public works')) as categories(category,department)
cross join (values ('Ward A'),('Ward B'),('Ward C')) as areas(area);
