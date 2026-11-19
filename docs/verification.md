# Verification — 19 November 2026

## Automated checks

- React TypeScript compilation and Vite production build: passed.
- Frontend workflow tests: 5 passed.
- Spring Boot workflow unit tests: 10 passed.
- Spring Boot MockMvc/H2 integration tests: 10 passed.
- Build output is split into application, React, authentication, and map chunks.

The 20 Java tests cover department/ward scope, unrelated citizens/officers, self-promotion denial, status rules, evidence requirements, reopening expiry, idempotent creation, stale updates, internal-note filtering, invalid coordinates, disguised image content, and closure/reopening history. JWT-authenticated MockMvc tests use test tokens; real Supabase signatures, mail delivery, PostgreSQL transactions and Storage uploads still require live environment verification.

## Browser

Tested the labelled browser-local demo at `http://127.0.0.1:5174`:

1. Submitted a sample streetlight report with description, landmark and coordinates.
2. Observed tracking number `CT-1C4CB450`, routing to Electrical maintenance, and a submission event.
3. Switched to the supervisor preview; the queue contained only the department/area's three sample reports.
4. Acknowledged the report with a reason, then assigned Ravi Kumar; both changes appeared in the timeline.
5. Tested the mobile viewport: document width and scroll width were both 375 pixels, with no horizontal page overflow. Opened the mobile navigation and Issue map.
6. No browser console errors were captured during that check.

`desktop-preview.jpg` captures the local demonstration, not a deployed production application. QA data remains only in this browser's localStorage.

## Supabase

- Dedicated project `wfchfajwkhkwmrutyvzp` is active.
- Six application tables have RLS enabled.
- Fifteen category/ward routing rules exist.
- Both `anon` and `authenticated` lack `USAGE` on the application schema.
- `civic-evidence` is private and has a 3 MB limit.
- The work-cycle timestamp column exists, allowing fresh resolution evidence to be required after work restarts.
- Advisor output contained only six informational notices about RLS tables with no policies. These are deliberately backend-only tables: https://supabase.com/docs/guides/database/database-linter?lint=0008_rls_enabled_no_policy

## Not yet verified

Live React → Spring → Supabase authentication/database/storage flow, Docker image execution, Vercel deployment, production email recovery, and GitHub CI execution. These need the repository and backend credentials/configuration. No real service-performance benefit has been measured.
