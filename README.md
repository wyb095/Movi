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

### 1. Install Android Studio
Use **Android Studio Ladybug (2024.2)** or newer.

### 1.1. Install Java 17
The project targets **Java 17**. Android Studio ships with a compatible JDK, or you can point `JAVA_HOME` to any local JDK 17 install.

### 2. Open the project
`File → Open…` and pick the `Movi/` folder. Gradle will sync automatically — first sync takes a few minutes.

### 3. Set up Firebase
1. Go to <https://console.firebase.google.com/> and create a new project called **Movi**.
2. Add an Android app with package name `com.group2.movi`.
3. Download `google-services.json` and drop it into `app/` (it's gitignored for security).
4. In the Firebase console, enable:
   - **Authentication → Email/Password** sign-in method
   - **Firestore Database** (start in test mode during development)
   - **Storage** (start in test mode during development)
   - **Cloud Messaging** (auto-enabled)

Important:
For team testing, every device must be built against the same shared Firebase project and the same `app/google-services.json`.
If different teammates each create their own local Firebase project, task posts, users, and chats will go to different Firestore databases and other devices will not see them.

### 4. Set up the Google Maps API key
1. Go to <https://console.cloud.google.com/>, enable **Maps SDK for Android**, create an API key.
2. Copy `local.properties.template` → `local.properties` and paste the key.

### 5. Run
Plug in an Android 8+ device (or start an emulator), hit the green **Run** button in Android Studio.

### 6. Gradle wrapper
This repo now includes the Gradle wrapper, so you do **not** need a globally installed `gradle` command.

- macOS / Linux: `./gradlew tasks`
- Windows: `gradlew.bat tasks`

If the shell says `Permission denied`, run `chmod +x ./gradlew` once.

---

## ✅ Verification

Use the wrapper for every local or CI verification step:

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

### Secret-less / CI verification

`google-services.json` is intentionally gitignored, so fresh clones and CI environments usually do not have Firebase config checked in.

For those environments, generate a placeholder config before running the wrapper:

```bash
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

The placeholder file is only for build reproducibility. Real devices and Firebase-backed features still need your actual `app/google-services.json`.

The repo also includes a minimal GitHub Actions workflow at `.github/workflows/android.yml` that runs the same wrapper-based verification chain, so local and CI entrypoints stay aligned.

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
