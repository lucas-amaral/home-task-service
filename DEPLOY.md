# Deployment Guide — Home Tasks

## Prerequisites
- A GitHub account
- A [Railway](https://railway.app) account
- A [Vercel](https://vercel.com) account

## 1. Push the Project to GitHub

```bash
cd home-task
git init
git add .
git commit -m "feat: initial home tasks app"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/home-task-service.git
git push -u origin main
```

## 2. Deploy the Backend to Railway

1. Open [railway.app](https://railway.app) and create a new project.
2. Choose `Deploy from GitHub repo` and select `home-task-service`.
3. Click `Add Service -> Database -> PostgreSQL`.
4. In the application service settings, set `Root Directory` to `backend`.
5. In `Variables`, add:

```text
DATABASE_URL=${{Postgres.DATABASE_URL}}
DATABASE_USERNAME=${{Postgres.PGUSER}}
DATABASE_PASSWORD=${{Postgres.PGPASSWORD}}
FRONTEND_URL=https://your-app.vercel.app
```

The production profile also supports Railway's native `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, and `PGPASSWORD` variables, so mapping the PostgreSQL service is usually enough even without extra datasource variables.

6. Railway will build and deploy the backend automatically.
7. In `Settings -> Domains`, copy the generated URL, for example `https://home-task-service.up.railway.app`.

## 3. Deploy the Frontend to Vercel

1. Open [vercel.com](https://vercel.com) and create a new project.
2. Import the `home-task` repository.
3. Configure the project with:
   `Root Directory`: `frontend`
   `Build Command`: `npm run build`
   `Output Directory`: `dist`
4. Add this environment variable:

```text
VITE_API_URL=https://home-task-service.up.railway.app
```

5. Click `Deploy`.
6. Copy the generated URL, for example `https://home-task.vercel.app`.

## 4. Update CORS on Railway

Go back to Railway and update:

```text
FRONTEND_URL=https://home-task-app.vercel.app
```

Railway will redeploy automatically.

## Local Development

```bash
# Terminal 1 - Backend
cd backend
./gradlew bootRun
# API at http://localhost:8080
# H2 console at http://localhost:8080/h2-console

# Terminal 2 - Frontend
cd frontend
cp .env.example .env.local
npm install
npm run dev
# App at http://localhost:5173
```

## Updating Later

Any `git push` to the `main` branch triggers automatic redeploys on Railway and Vercel.

```bash
git add .
git commit -m "feat: new task"
git push
```

## API Endpoints

| Method | Route | Description |
|--------|------|-------------|
| GET  | `/api/health` | Health check |
| GET  | `/api/board?weekStart=2024-01-15` | Weekly board |
| GET  | `/api/tasks` | List tasks |
| POST | `/api/tasks` | Create task |
| POST | `/api/assignments/assign` | Assign task |
| POST | `/api/assignments/{id}/complete` | Mark as completed |
| POST | `/api/assignments/{id}/uncomplete` | Undo completion |
| POST | `/api/assignments/{id}/penalty` | Apply penalty |
| GET  | `/api/rewards` | List rewards |
| GET  | `/api/points/history` | Points history |
