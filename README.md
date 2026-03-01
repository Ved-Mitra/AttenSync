# AttenSync

AttenSync is an Android-based productivity and focus-tracking application designed to help users monitor screen time, manage app usage through a rewards-based leaderboard, and engage in community discussions.

## 🚀 Key Features

* **Screen Time Dashboard**: Shows aggregate screen time for monitored apps with detailed per-app breakdowns.
* **Focus Tracking Service**: A dedicated foreground service monitors app usage and focus sessions in real-time.
* **Gamified Leaderboard**: Earn points based on usage stats and view global rankings. Points are cached locally for offline access.
* **Community Discussion**: Integrated real-time chat platform using Socket.io to discuss productivity tips.
* **Secure Auth**: Quick onboarding using Google Sign-In and Firebase Authentication.

## 🛠️ Technical Stack

### Android (Client)
* **Language**: Kotlin.
* **Architecture**: MVVM with ViewBinding and Navigation Component.
* **Networking**: Retrofit 2.9.0 with Gson for API communication.
* **Image Loading**: Glide for profile and UI assets.
* **Real-time**: Socket.io client for the discussion module.

### Backend (Server)
* **Environment**: Node.js (Express).
* **Storage**: File-based JSON state management (`leaderboard.json`).
* **API**: RESTful endpoints for usage reporting (`/v1/usage`) and leaderboard retrieval (`/v1/leaderboard`).

## 📋 Prerequisites & Setup

### Permissions
The app requires the following key permissions to be granted:
* `PACKAGE_USAGE_STATS`: Required to read app usage statistics.
* `POST_NOTIFICATIONS`: For focus reminders and alerts.
* `FOREGROUND_SERVICE`: To maintain tracking while the app is in the background.

### Server Setup
1.  Navigate to the `backend/` directory.
2.  Install dependencies: `npm install`.
3.  (Optional) Seed initial data: `npm run seed`.
4.  Start the server: `npm start`.
    * *Default port is 8080 unless specified in environment variables.*

### Android Configuration
Update `LeaderboardService.BASE_URL` in the Android source code to point to your specific server address before building the APK.

## 🧪 Testing

Run the local unit test suite using Gradle:

```bash
./gradlew testDebugUnitTest
