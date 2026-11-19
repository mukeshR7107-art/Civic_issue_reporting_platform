# CivicTrack

A civic issue reporting and resolution platform built with **React + TypeScript**, **Spring Boot**, **Supabase PostgreSQL/Auth/Storage**, and a **Vercel container deployment**.

This is an initial implementation, not an official municipal complaint channel. The sample city is labelled throughout the demo. No sample complaints are sent to municipal staff.

## Run the frontend

Requirements: Node.js 22.12+ (Node 22 is recommended).

```powershell
npm ci
Copy-Item .env.example .env.local
npm run dev
```

`VITE_DEMO_MODE=true` enables a browser-local demo with four role previews. The demo stores sample reports and photos in localStorage. It never calls Spring or persists complaints to Supabase. A production build must use `VITE_DEMO_MODE=false`; there is no automatic fallback from a failed API request to demo data.

The development server proxies `/api` to `http://localhost:8080`. `VITE_API_URL` can point to a separately hosted API. In the Vercel container, Spring serves the React build and `/api` from the same origin.

## Run the backend

Requirements: Java 21+ and Maven 3.9+. The project targets Java 21.

1. Copy `backend/.env.example` to `backend/.env` and fill it locally. Never commit this file or paste credentials into chat.
2. Obtain the **session pooler JDBC connection details** from the Supabase project's Connect dialog. Use SSL. Keep the connection pool small (five connections by default).
3. Set `SUPABASE_SERVICE_KEY` to a server-only Supabase secret/service-role key. This is used only for private image storage. Never prefix it with `VITE_`.
4. Run `./scripts/start-backend.ps1`, or export those environment variables and run `mvn -f backend/pom.xml spring-boot:run`.

Required server variables:

| Variable | Purpose |
|---|---|
| `DATABASE_URL` | PostgreSQL JDBC session-pooler URL, with `sslmode=require` |
| `DATABASE_USERNAME` | Server database username from Connect |
| `DATABASE_PASSWORD` | Server database password |
| `SUPABASE_URL` | `https://wfchfajwkhkwmrutyvzp.supabase.co` |
| `SUPABASE_PUBLISHABLE_KEY` | Public `sb_publishable_...` key exposed through `/api/config` |
| `SUPABASE_SERVICE_KEY` | Legacy `service_role` JWT beginning `eyJ`, for direct private Storage REST access; backend only |
| `ALLOWED_ORIGIN` | Exact frontend origin; defaults to `http://localhost:5173` |
| `PORT` | HTTP port; defaults to 8080 |

Spring validates JWT signatures through Supabase JWKS, the issuer, expiry, and the `authenticated` audience. Configure an asymmetric signing key (ES256 or RS256) in Supabase Auth. Legacy HS256 JWT secrets are intentionally not accepted. Roles are read from the server-owned `civictrack.profiles` table, never from user-editable JWT metadata.

## Database

Dedicated Supabase project: **CivicTrack**, ref `wfchfajwkhkwmrutyvzp`, region `ap-south-1`.

The initial schema and private evidence bucket were applied to this project. CRS was not changed.

- `backend/src/main/resources/db/migration/` contains Flyway schema migrations.
- `database/storage.sql` creates the private bucket.
- A non-exposed `civictrack` schema contains profiles, routing rules, complaints, timeline events, attachments, and notifications.
- All tables enable RLS. `anon` and `authenticated` have no schema access or row policies. All report access is through Spring's authorization checks. The initial JDBC configuration uses the trusted database owner; never expose its credentials. A least-privilege runtime role and separate migration owner are recommended before a real pilot.
- Supabase's advisor reports informational `rls_enabled_no_policy` notices for these deliberately closed tables. Do not add permissive browser policies to silence them.
- Flyway baselines the already-provisioned schema at version 1. On a fresh database it runs V1 normally. Do not point this project at an unrelated database/schema.

Configure Supabase Auth's site URL and allowed redirect URLs to match your frontend deployment and local development origin. Enable email/password authentication and configure a production SMTP provider before a real pilot.

### First administrator

Register and sign in once, which creates a citizen profile. A project owner must then explicitly provision the first administrator in the Supabase SQL editor:

```sql
update civictrack.profiles
set role = 'ADMIN', name = 'Your name'
where id = 'YOUR_AUTH_USER_UUID';
```

Verify the UUID first. There is no self-promotion endpoint. Administrators can provision other registered users through the administration UI; they cannot change their own permissions there.

## Implemented workflows

- Email/password registration, sign-in, recovery, and sign-out through Supabase Auth.
- Report submission with tracking ID, category, ward, description, location coordinates/map pin, perceived urgency, and optional photograph.
- Category + ward routing, including an unassigned state when no rule matches.
- Citizen-owned reports, department-and-area supervisor queues, assigned-officer access, and administrator access.
- Search, status filtering, paging, map display, progress discussions, and internal staff notes.
- Acknowledgement, requests for clarification, reasoned rejection, assignment/reassignment, in-progress updates, photographic resolution proposals, citizen confirmation, and reopening.
- Optimistic locking on assignment/status/priority updates; idempotency keys on report creation.
- Private JPEG/PNG evidence, maximum 3 MB and 20 megapixels; backend checks file signatures and removes metadata by re-encoding. Short-lived signed reads require report access.
- Transactional in-app notifications, including assignment notifications. Notification writes commit with the report change.
- Scoped analytics showing the currently loaded page, with explicit metric definitions.
- Staff provisioning and category/ward routing administration.

### Explicit policies

- Only responsible supervisors or administrators acknowledge/reject a submitted report or request information.
- Officers can only start assigned work and propose resolution with evidence.
- The reporter confirms closure. An officer's proposal is not closure.
- Reopening requires a reason within seven days of the proposal. Reopened work goes through staff assignment.
- No automatic closure for an unresponsive citizen.
- Reassignment preserves the original 72-hour demonstration target.
- Submitted content is not silently edited after assignment; citizens add clarifications to the timeline.
- Comments marked internal are excluded from citizen detail responses.
- Browser role previews exist only in explicit demo mode and confer no backend privileges.

## Vercel

`Dockerfile.vercel` builds React, embeds `dist` into the Spring Boot jar, and runs Java 21 as a non-root user. `vercel.json` selects the container runtime in Mumbai. Vercel's container runtime is currently beta; confirm its availability in the target project.

1. Connect the GitHub repository to a **new CivicTrack Vercel project**, with the repository root as Root Directory.
2. Set the backend variables above in Vercel, with `PORT=8080` and `ALLOWED_ORIGIN` equal to the deployment origin.
3. Set `SUPABASE_PUBLISHABLE_KEY` to the public `sb_publishable_...` key. Set `SUPABASE_SERVICE_KEY` to the legacy JWT-style `service_role` key beginning `eyJ`; the newer `sb_secret_...` key cannot be used as a Storage `Authorization: Bearer` token by this direct REST client. The frontend obtains public configuration from `/api/config` at runtime; Docker build arguments are optional. Never provide a secret key to the frontend build.
4. Deploy, then configure the Supabase Auth redirects for the resulting URL.
5. Run the live sign-in → report → assignment → evidence → confirmation workflow using provisioned test accounts before accepting real reports.

Vercel project: `civictrack` (`prj_NO29b4R83EALaBIlT5YLnEzVtdlY`), in `mukeshr7107-1321s-projects`. Public Supabase configuration and port 8080 are set. The container config is prepared, but no live backend deployment has been verified yet. Server database/storage credentials must be configured securely first.

## Verification

```powershell
npm run build
npm test
mvn -f backend/pom.xml test
```

Backend tests cover role boundaries, invalid transitions, evidence requirements, reopening expiry, protected API access, metadata-based escalation, department access, internal-note privacy, idempotent creation, validation, and stale assignment versions. Integration tests use H2 in PostgreSQL compatibility mode; they do not replace tests against Supabase PostgreSQL/JWKS/Storage.

Browser checks cover demo submission, acknowledgement, assignment, and mobile navigation. See `docs/desktop-preview.jpg`.

## Remaining before a real pilot

- Configure and verify live JDBC, Auth redirects, and private Storage credentials end-to-end.
- Live staff accounts and administrator provisioning; production deployment and GitHub CI connection.
- Configurable area/department/category catalogs beyond the initial three wards and five categories.
- Scheduled escalation, email delivery/retries, aggregate database analytics, moderation, and production rate limits.
- Administrative configuration audit events, formal assignment/feedback entities, attachment orphan cleanup, and automated PostgreSQL/Storage integration tests.
- Accessibility/user testing, backups, retention policies, and an agreement with staff who actually handle reports.

Do not claim measured service improvements or real municipal adoption from the sample dataset.
