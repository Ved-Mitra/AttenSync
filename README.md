# AttenSync

Dashboard screen timer bar

- Shows aggregate screen time for monitored apps on the dashboard.
- Tap the card to see per-app times.
- Requires Usage Access permission to read app usage stats.

Leaderboard

- Backend API lives in `backend/` (Express + SQLite).
- Android uses Retrofit; update `LeaderboardService.BASE_URL` for your server.
- Points are cached locally; offline shows the last known value.

## Tests

```bash
./gradlew testDebugUnitTest
```
