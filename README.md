# Home Tasks

Household task dashboard for kids, with points and rewards.

## Structure

```text
home-task/
├── service (backend)/     # Kotlin + Spring Boot (Railway)
└── app (frontend)/    # React + TypeScript (Vercel)
```

## Deployment

### Backend on Railway
1. Create an account at [railway.app](https://railway.app).
2. Create a new project and deploy from GitHub, then select the `backend` folder.
3. Add the environment variable `FRONTEND_URL=https://your-app.vercel.app`.
4. Railway will detect the Kotlin project automatically through `build.gradle.kts`.

### Frontend on Vercel
1. Create an account at [vercel.com](https://vercel.com).
2. Create a new project from this repository and set the root directory to `frontend`.
3. Add the environment variable `VITE_API_URL=https://your-backend.railway.app`.
4. Every push to GitHub will trigger an automatic deployment.

## Local Development

### Backend
```bash
cd backend
./gradlew bootRun
# Runs on http://localhost:8080
```

### Frontend
```bash
cd frontend
npm install
npm run dev
# Runs on http://localhost:5173
```

## Features
- Dashboard with sticky notes by type: daily, weekly, shared, and rules
- Drag-and-drop task assignment between Clara and Bernardo
- Points system with weekly history
- Configurable rewards and consequences
- Time-based tasks with visual deadline alerts
- PostgreSQL persistence on Railway
