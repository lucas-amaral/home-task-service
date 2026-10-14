# Deployment Guide — home-task-service (Backend)

Railway's free trial expired, so this backend now deploys to **Render** (free web
service tier, no card required, doesn't expire) with the database on
**Neon** or **Supabase** (permanent free Postgres — Render's own free Postgres
expires after 30 days, so we don't use it).

## Prerequisites
- A GitHub account with this repo pushed
- A [Render](https://render.com) account
- A [Neon](https://neon.tech) **or** [Supabase](https://supabase.com) account (either works — pick one)

## 1. Create the database (Neon or Supabase)

**Neon:**
1. Create a new project.
2. Copy the connection string shown (it looks like
   `postgres://user:pass@ep-xxx.neon.tech/dbname?sslmode=require`).

**Supabase:**
1. Create a new project (set a database password when prompted).
2. Go to `Project Settings → Database → Connection string → URI`, copy it
   (looks like `postgres://postgres:[password]@db.xxx.supabase.co:5432/postgres`).

Either format works as-is — the backend parses it automatically
(`config/DatabaseConfig.kt`), adding `sslmode=require` if it's missing.
You do **not** need to prefix it with `jdbc:` yourself.

## 2. Deploy the backend to Render

**Option A — Blueprint (recommended):**
1. Push this repo to GitHub.
2. In Render, click `New → Blueprint`, point it at the repo. It will read
   `render.yaml` and set up the web service automatically (free plan, Docker
   runtime, health check on `/api/health`).
3. Fill in the env vars it asks for (see below).

**Option B — Manual:**
1. `New → Web Service` → connect the repo.
2. Runtime: `Docker` (uses the included `Dockerfile`).
3. Plan: `Free`.
4. Health Check Path: `/api/health`.

### Required environment variables

```text
SPRING_PROFILES_ACTIVE = prod
DATABASE_URL            = <connection string from step 1>
FRONTEND_URL            = https://your-app.vercel.app
TZ                      = America/Sao_Paulo
CHILD1_NAME             = Clara
CHILD2_NAME             = Bernardo
```

### Optional — WhatsApp deadline reminders (Twilio)

```text
TWILIO_ACCOUNT_SID = ...
TWILIO_AUTH_TOKEN  = ...
TWILIO_WA_FROM      = whatsapp:+1415...
WHATSAPP_CHILD1     = whatsapp:+55...
WHATSAPP_CHILD2     = whatsapp:+55...
WHATSAPP_PARENTS    = whatsapp:+55...
```

Leave these unset to skip WhatsApp notifications entirely — nothing else
depends on them.

### Daily jobs and push reminders

```text
VAPID_PUBLIC_KEY  = <from ./gradlew generateVapidKeys>
VAPID_PRIVATE_KEY = <from ./gradlew generateVapidKeys>
VAPID_SUBJECT     = mailto:<a contact e-mail>
```

- `./gradlew generateVapidKeys` prints a new key pair. Generate it **once**: changing
  the keys invalidates every phone already subscribed.
- The free instance sleeps, so the in-app `@Scheduled` jobs only fire when it happens
  to be awake. To cover that, `WakeUpCatchUp` runs on every start: it closes yesterday,
  creates today's tasks and sends the reminder of the current window (noon: 12:00–19:00,
  evening: 19:00–22:00) if it has not gone out yet. Everything is idempotent.
- So the reminders only need *something* to wake the service at 12:00 and 19:00
  (Brasília). Use a scheduled-URL service made for that, e.g. [cron-job.org](https://cron-job.org)
  (free): two jobs doing `GET https://<service>.onrender.com/api/health`, one at 12:00 and one
  at 19:00, timezone America/Sao_Paulo. If nobody wakes it, the reminders simply go out the
  next time someone opens the board inside the window.
- **Do not use GitHub Actions for this.** GitHub's terms forbid using hosted runners for
  activity unrelated to building, testing, deploying or publishing the repository's
  software — scheduled pings/keep-alive included — and the account can be suspended.
- Optional: `CRON_TOKEN` (e.g. `openssl rand -hex 32`) enables `POST /api/cron/daily` and
  `POST /api/cron/reminders?slot=NOON|EVENING` with the header `X-Cron-Token`, to run the
  jobs by hand. Without it those endpoints answer 503. Without the VAPID keys the
  reminders are skipped (and not logged, so they go out once the keys are set).

5. Deploy. Render builds the Docker image and starts the service.
6. Copy the generated URL, e.g. `https://home-task-service.onrender.com`.

> **Free tier note:** the service spins down after ~15 min of inactivity and
> takes 30–60s to wake up on the next request. That's fine for a household
> board — the very first tap of the day might just take a moment.

## 3. Point the frontend at the backend

In Vercel (frontend project), set:

```text
VITE_API_URL = https://home-task-service.onrender.com
```

Redeploy the frontend so it picks up the new URL.

## 4. First run — check the seed data

On first boot with an empty database, `DataSeeder` populates the 6 house
tasks and family config automatically. If you're migrating from an old
database that still has the *previous* task list (points-based, no
checklist), the seeder won't touch it — it only seeds when the tasks table
is empty. In that case, either:
- point `DATABASE_URL` at a **fresh** empty database (simplest), or
- clear out the old tasks via the Admin page / `DELETE /api/tasks/{id}`, then
  restart the service so it reseeds.

## Local Development

```bash
./gradlew bootRun
# API at http://localhost:8080 (H2 in-memory db, no setup needed)
# H2 console at http://localhost:8080/h2-console (enable in application.properties)
```

## Updating Later

Any `git push` to `main` triggers an automatic redeploy on Render.

## API Endpoints

| Method | Route | Description |
|--------|------|-------------|
| GET  | `/api/health` | Health check |
| GET  | `/api/board?date=2026-08-13` | Today's board (assignments + weekly status) |
| GET  | `/api/weeks/{weekStart}` | Full week summary |
| GET  | `/api/tasks` | List tasks |
| POST | `/api/tasks` | Create task |
| POST | `/api/assignments/assign` | Assign task |
| POST | `/api/assignments/{id}/complete` | Mark as completed (no points — punitive model) |
| POST | `/api/assignments/{id}/uncomplete` | Undo completion |
| POST | `/api/assignments/{id}/penalty` | Register a −1 occurrence (not done / late / incomplete) |
| DELETE | `/api/assignments/{id}` | Delete an assignment (reverses penalty if one was applied) |
| GET  | `/api/rewards` | List rewards (optional, not tied to the automatic system) |
| GET  | `/api/points/history` | Points history by week |
| GET  | `/api/points/status?weekStart=` | Occurrence count + consequence ladder per child |
| POST | `/api/cron/daily` | Closes yesterday (missed → penalty) and creates today's assignments. Header `X-Cron-Token` |
| POST | `/api/cron/reminders?slot=NOON\|EVENING` | Pushes each child the tasks still open today (once per slot/day). Header `X-Cron-Token` |
| GET  | `/api/push/public-key` | VAPID public key and whether push is enabled |
| POST | `/api/push/subscriptions` | Subscribes a device `{endpoint, p256dh, auth, assignee: CHILD1\|CHILD2}` |
| DELETE | `/api/push/subscriptions` | Unsubscribes a device `{endpoint}` |
| POST | `/api/push/test` | Sends a test notification to a device `{endpoint}` |
