# 📍 GeoTrace — Location Geofencing App

An Android application that lets you create location-based geofences, plan multi-stop routes, and track visits using entry and exit events — even when the app runs in the background.

---

## ✨ Features

- 🗺️ Interactive map powered by MapLibre (OpenStreetMap)
- 📌 Long-press the map to add a geofence with auto-resolved address name
- 👆 Tap geofence circles to select them for a route
- 🧭 Multi-stop route planning with nearest-first ordering
- 🛣️ Real road route via OSRM with distance and ETA per leg
- 🔔 Entry & exit notifications with time spent inside each geofence
- ✅ Auto-advance to next stop when you arrive
- 🔄 Background tracking via Foreground Service — works with app closed
- 📋 Full visit history with entry time, exit time and duration
- 🔁 Restarts automatically after device reboot

---

## 🛠️ Tech Stack

| Category | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM + Clean Architecture |
| Maps | MapLibre (OpenFreeMap tiles) |
| Location | Fused Location Provider API |
| Routing | OSRM (Open Source Routing Machine) |
| Background Tracking | Foreground Service + WakeLock |
| Dependency Injection | Hilt |
| Database | Room |
| Navigation | Navigation3 |
| Notifications | NotificationManager |

---

## 📦 Download APK

👉 [Download Release APK](https://github.com/AtulGupta8097/GeoTrace/releases/download/v1.1/app-release.apk)

---

## 🚀 How to Use

Long-press the map to add geofence locations, tap them to select for a route, then press **"Start Route"** — the app will guide you and notify you at each stop.
Check the **Geofences** tab to manage saved locations, **Route** tab to track progress, and **Visits** tab to see your history.

---

## 🏃 How to Run the Project

1. Clone the repository
   ```bash
   git clone https://github.com/AtulGupta8097/GeoTrace.git
   ```

2. Open in Android Studio

3. Sync Gradle and run on a device or emulator with Google Play Services

> ⚠️ A physical device is recommended — location and background service behaviour is unreliable on emulators.

---

## 📋 Permissions Required

| Permission | Reason |
|---|---|
| `ACCESS_FINE_LOCATION` | Show user position on map |
| `ACCESS_BACKGROUND_LOCATION` | Track geofences when app is closed |
| `POST_NOTIFICATIONS` | Entry / exit alerts |
| `FOREGROUND_SERVICE_LOCATION` | Background location service |
| `RECEIVE_BOOT_COMPLETED` | Restart service after reboot |

---
