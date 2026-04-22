# Movi — Cross-Border P2P Delivery (Android)

**Group 2** · Huang Zhan · Lee Victor · Liu Yunfeng · Lyu Zhongyan · Wang Wanlin · Wang Yubo

Movi matches cross-border commuters between Hong Kong and Shenzhen with people who need small items delivered. Carriers earn pocket money on trips they were already making; requesters get same-day, low-cost micro-delivery.

---

## 🏗️ What's in this repo

A complete Android Studio project skeleton built with:

- **Kotlin + Jetpack Compose** — modern declarative UI
- **MVVM + Clean Architecture** — separated `ui/` → `domain/` → `data/`
- **Hilt** — dependency injection
- **Firebase** — Auth, Firestore, Storage, Cloud Messaging (FCM)
- **Google Maps Compose** — live task discovery map with task markers
- **Coil** — image loading

All four repositories (Auth, User, Task, Chat, Review) are wired up to real Firestore calls. Every screen listed in the planning document is implemented as a real composable screen with a ViewModel.

## 📱 Screens implemented

| Module | Screens |
|--------|---------|
| **Auth** | Splash, Login, Register, Email Verify |
| **Home** | Smart-matched task feed, detour filter, map view, Task Detail with accept + escrow |
| **Post** | 4-step wizard (category/desc → locations → port+timing → price+review) |
| **My Tasks** | Tasks as Requester / As Carrier tabs |
| **Task Progress** | Status stepper (POSTED → ACCEPTED → PICKED UP → DELIVERED → CONFIRMED) |
| **Chat** | Real-time Firestore chat, text + invoice/item photo messages |
| **Delivery Confirm** | PIN / QR-token verification, escrow release, two-way reviews, dispute |
| **Profile** | Profile header, stat cards, menu rows |
| **Edit Profile** | Name + profile photo upload |
| **Schedule** | Add/delete commute schedule entries |
| **Earnings** | Weekly / monthly / all-time + completed tasks list |
| **Reviews** | List of reviews received |

---

## 🚀 Getting started

### Team setup contract

For shared testing and demo, everyone should use the same backend config:

- the same shared `app/google-services.json`
- the same shared `MAPS_API_KEY` in `local.properties`

Do **not** commit either one to GitHub. Share them with teammates through a private channel.

### 1. Install Android Studio

Use **Android Studio Ladybug (2024.2)** or newer.

### 1.1. Install Java 17

The project targets **Java 17**. Android Studio ships with a compatible JDK, or you can point `JAVA_HOME` to any local JDK 17 install.

### 2. Open the project

Open `Movi/` in Android Studio with `File → Open…`. The first Gradle sync may take a few minutes.

What should happen on first open:

- Android Studio usually creates a local `local.properties` file with your machine's `sdk.dir`.
- That file is local-only and should stay untracked.
- If Android Studio does **not** create it, copy `local.properties.template` to `local.properties` and fill in `sdk.dir` yourself.

### 3. Add the shared Firebase config

1. Ask the team for the shared `app/google-services.json`.
2. Place it at `app/google-services.json`.
3. Re-sync if Android Studio asks.

Important:

- `google-services.json` is gitignored on purpose.
- If teammates create separate Firebase projects, users, tasks, chats, and reviews will be split across different Firestore databases and you will not see each other's data.

### 4. Add the shared Google Maps key

1. Open your local `local.properties`.
2. Add the shared team key:

```properties
MAPS_API_KEY=your_shared_team_key
```

3. If `local.properties` does not exist yet, copy `local.properties.template` to `local.properties`, then fill in both `sdk.dir` and `MAPS_API_KEY`.

Without a real Maps key, the project still compiles and the app still opens, but:

- Home `Map` view is disabled
- Places autocomplete is disabled
- teammates can still paste Google Maps links or raw `lat,lng` coordinates when posting tasks or adding commute routes

### 5. Run

Start an emulator or connect an Android 8+ device, then hit the green **Run** button in Android Studio.

### 6. Gradle wrapper

Use the Gradle wrapper included in the repo instead of a globally installed `gradle` command.

- macOS / Linux: `./gradlew tasks`
- Windows: `gradlew.bat tasks`

If the shell says `Permission denied`, run `chmod +x ./gradlew` once.

### First-open checklist

Before you start feature work, confirm these local-only files exist and are **not** staged in Git:

- `local.properties` with your `sdk.dir`
- `app/google-services.json` from the team
- optional `MAPS_API_KEY` line inside `local.properties`

---

## ✅ Verification

### Android Studio / local verification

Use the wrapper for every local verification step:

```bash
./gradlew app:compileDebugKotlin
./gradlew app:testDebugUnitTest
./gradlew app:lintDebug
./gradlew app:build
```

You can also run the full local verification chain in one command:

```bash
./gradlew app:compileDebugKotlin app:testDebugUnitTest app:lintDebug app:build
```

### CLI / CI verification

For pure command-line or CI environments, create local config explicitly before running the wrapper.

If `local.properties` is missing, create it with your Android SDK path:

```bash
printf "sdk.dir=%s\nMAPS_API_KEY=DISABLED\n" "$ANDROID_SDK_ROOT" > local.properties
```

`MAPS_API_KEY=DISABLED` is a safe placeholder that keeps the build reproducible while intentionally disabling map search/view.

`google-services.json` is also intentionally gitignored, so fresh clones and CI should generate a placeholder file before building:

```bash
printf "sdk.dir=%s\nMAPS_API_KEY=DISABLED\n" "$ANDROID_SDK_ROOT" > local.properties

cat > app/google-services.json <<'JSON'
{
  "project_info": {
    "project_number": "1234567890",
    "project_id": "movi-ci",
    "storage_bucket": "movi-ci.appspot.com"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:1234567890:android:abcdef123456",
        "android_client_info": {
          "package_name": "com.group2.movi"
        }
      },
      "oauth_client": [],
      "api_key": [
        {
          "current_key": "AIzaSyCI_PLACEHOLDER_KEY"
        }
      ],
      "services": {
        "appinvite_service": {
          "other_platform_oauth_client": []
        }
      }
    }
  ],
  "configuration_version": "1"
}
JSON

./gradlew app:compileDebugKotlin app:testDebugUnitTest app:lintDebug app:build
```

The placeholder file is only for build reproducibility. Real devices and Firebase-backed features still need the team's actual `app/google-services.json`.

The repo also includes a minimal GitHub Actions workflow at `.github/workflows/android.yml` that runs the same wrapper-based verification chain with placeholder local config, so local and CI entrypoints stay aligned.

### Common setup issues

- `SDK location not found`
  Android Studio did not create `local.properties`, or `sdk.dir` points to the wrong Android SDK location. Copy `local.properties.template` if needed and fix the path.
- `google-services.json is missing`
  Ask the team for the shared Firebase config and place it at `app/google-services.json`.
- `MAPS_API_KEY` not configured
  The app will still open, but map view and Places autocomplete stay disabled until you add the shared team key to `local.properties`.
- Firebase Auth / Firestore / Storage features fail at runtime
  Confirm everyone is using the same Firebase project, and check the Firebase console rules plus enabled services for Auth, Firestore, Storage, and Messaging.

---

## 🗃️ Firestore schema

```
users/{uid}
  displayName, email, profilePhotoUrl, role[], isVerified,
  rating, totalReviews, totalEarnings, tasksCompleted,
  fcmToken, commuteSchedule[], createdAt

tasks/{taskId}
  requesterId, requesterName, requesterRating,
  carrierId?, carrierName?,
  status (OPEN | ACCEPTED | PICKED_UP | DELIVERED | CONFIRMED | CANCELLED | DISPUTED),
  category (FOOD | PARCEL | DOCUMENT | MEDICINE | OTHER),
  title, description, itemPhotoUrl?,
  pickupLocation (GeoPoint), pickupAddress,
  dropoffLocation (GeoPoint), dropoffAddress,
  crossingPort (FUTIAN | LO_WU | HUANGGANG | LOK_MA_CHAU | HEUNG_YUEN_WAI),
  direction (HK_TO_SZ | SZ_TO_HK),
  requiredBefore (Timestamp),
  offeredPrice, finalPrice?,
  escrowHeld, escrowStatus, escrowHoldAmount,
  deliveryPin, deliveryQrToken,
  customsDeclaration, isUrgent,
  requesterReviewed, carrierReviewed,
  createdAt, acceptedAt?, pickedUpAt?, deliveredAt?,
  escrowHeldAt?, escrowReleasedAt?

  tasks/{taskId}/messages/{msgId}
    senderId, senderName, text, messageType, imageUrl?, imageLabel?, sentAt, isRead

reviews/{reviewId}
  taskId, reviewerId, reviewerName, revieweeId,
  reviewerRole, revieweeRole, rating, comment, createdAt
```

---

## 🧑‍💻 Module ownership (6 team members)

| Member | Module | Package |
|--------|--------|---------|
| 1 | Auth | `ui/auth/` |
| 2 | Home feed + Task detail | `ui/home/` |
| 3 | Post task wizard | `ui/post/` |
| 4 | My tasks + progress + chat | `ui/tasks/` |
| 5 | Profile + schedule + earnings + reviews | `ui/profile/` |
| 6 | Plumbing: DI, nav, FCM, data layer | `di/`, `ui/navigation/`, `service/`, `data/` |

Each module is self-contained. Multiple members can work in parallel without merge conflicts as long as they stay within their own package.

---

## 📋 Sprint plan

| Sprint | Duration | Goal |
|--------|----------|------|
| Sprint 0 | Day 1–2 | Open project, sync gradle, set up Firebase, run on one device |
| Sprint 1 | Week 1 | Auth flow fully working, Home feed shows live tasks from Firestore |
| Sprint 2 | Week 2 | Post task end-to-end, accept flow, real-time chat |
| Sprint 3 | Week 3 | Delivery confirmation + reviews, schedule management, earnings |
| Sprint 4 | Week 4 | FCM push notifications, compliance alerts, UI polish, demo prep |

---

## ⚠️ Important notes

- **Escrow is implemented inside Firestore transactions.** Accepting a task now freezes the offered price, generates a delivery PIN + QR-token payload, and only releases funds after requester verification.
- **Smart matching is now route-aware on device.** Tasks are ranked from commute schedule + port + deadline + detour estimate. Best results come from pasting Google Maps links or raw coordinates when posting.
- **Customs compliance** still uses an explicit acknowledgement flow, but it is now paired with escrow and handoff verification instead of being the only safety control.

## 🧪 Demo failure-message checklist

Use these messages consistently during demo and testing:

- **Task already accepted**: "This task was just accepted by another carrier."
- **Network failure (send chat)**: "Network error. Message was not sent."
- **Network failure (status update)**: "Network error. Please check your connection and try again."
- **Delivery confirm failed**: "Failed to confirm delivery."
- **Review submit failed**: "Delivery confirmed, but review failed to submit. Please try again."
- **Notification permission denied**: "Notifications are off. You may miss task/chat updates."

Good luck! 🚀
