# hisync — Android Client

## Overview
hisync is a band practice management mobile app built for Android. It helps bands organize rehearsals, manage song lineups, assign practice tasks to members, and track recording submissions — all in one place.

---

## Tech Stack
- **Language:** Java
- **Min SDK:** 26 (Android 8.0 Oreo) | **Target SDK:** 34
- **Architecture:** Single-Activity + Fragment-based navigation
- **Networking:** Retrofit2 + OkHttp + Gson
- **Image loading:** Glide
- **UI:** Material Components 3, ViewPager2, SwipeRefreshLayout
- **Media:** MediaPlayer (audio playback)
- **Theme:** AppCompatDelegate (light/dark mode toggle)

---

## Prerequisites

| Tool | Version | Download |
|------|---------|----------|
| Android Studio | Hedgehog 2023.1+ | https://developer.android.com/studio |
| JDK | 17+ | bundled with Android Studio |
| Android device or emulator | API 26+ | — |
| hisync backend | running locally | see backend README |

---

## Installation & Setup

### Step 1 — Open the project
1. Open Android Studio
2. Click **File → Open** and select the `app/` project folder
3. Wait for Gradle sync to complete

### Step 2 — Configure the server URL
The app connects to the backend over your local network. You need to tell the app where the backend is running.

Open `app/build.gradle` and find the `buildConfigField` for `SERVER_URL`:

```gradle
buildConfigField "String", "SERVER_URL", '"http://YOUR_IP:8080/api/"'
```

Replace `YOUR_IP` with your computer's local IP address (the same one from the backend setup — e.g. `172.20.10.2`).

> **Important:** Your Android device and your computer must be on the **same network** (same WiFi or the phone connected to the computer's hotspot).

### Step 3 — Connect your device
**Option A — Physical device (recommended):**
1. On your Android phone, go to **Settings → Developer Options → USB Debugging** → enable it
2. Connect your phone via USB
3. Accept the debugging prompt on your phone

**Option B — Emulator:**
1. In Android Studio, open **Device Manager** (right toolbar)
2. Create a new virtual device (API 26+, any screen size)
3. Start the emulator

> **Note for emulator users:** Use `10.0.2.2` instead of your local IP in the `SERVER_URL` — this is the emulator's alias for `localhost`.

### Step 4 — Build and run
1. Select your device from the device dropdown at the top of Android Studio
2. Click the green ▶ **Run** button (or press `Shift + F10`)
3. The app will build and install automatically

---

## First-Time Usage Guide

### For the Band Leader

#### 1. Create an account
- Open the app → tap **Sign up**
- Enter your email and a password (minimum 6 characters)
- Tap **Create Account**

#### 2. Set your instruments
- After registering, you'll be asked **"What do you play?"**
- Select all instruments you play (Guitar, Bass, Drums, etc.)
- Tap **Continue**

#### 3. Go through onboarding
- Swipe through the 4 onboarding screens to learn the app layout
- Tap **Get Started** when done

#### 4. Create your band
- Tap **I'm a Leader**
- Enter your band name and an optional description
- Tap **Create Band**
- You'll be taken to the main app — your band's **invite code** will be shown in the Band tab

#### 5. Invite your members
- Go to the **Band** tab
- Share the **invite code** with your band members (tap **Copy** to copy it)

#### 6. Create a lineup
- Go to the **Lineups** tab
- Tap the **+** button
- Search YouTube for your song → tap it to select
- Check the members participating and set their instrument for this song
- Tap **Save Lineup**

#### 7. Schedule a session
- Go to the **Band** tab → tap **New Session**
- Pick a lineup from the dropdown
- Tap **Pick date & time** → select the rehearsal date and time
- Select the duration
- Tap **Create Session**

#### 8. Assign tasks
- Go to the **Band** tab → tap **Manage Tasks**
- Find the session → tap **+ Add task**
- Pick a member, type the task title (e.g. "Record guitar intro")
- Tap **Save Task**

#### 9. Review submissions
- When a member submits a recording, their task status changes to **Awaiting Approval**
- Go to **Manage Tasks** → tap the task → listen to the recording
- Approve or request a re-record *(Phase 5 — coming soon)*

---

### For Band Members

#### 1. Create an account
- Open the app → tap **Sign up**
- Enter your email and password → tap **Create Account**

#### 2. Set your instruments
- Select the instruments you play → tap **Continue**

#### 3. Join your band
- Tap **I'm a Member**
- Enter the invite code your leader gave you
- Tap **Join Band**

#### 4. Check your schedule
- Go to the **Schedule** tab
- Swipe left/right to navigate between weeks
- Tap a session chip to see the lineup and tasks for that session

#### 5. View and complete your tasks
- Go to the **Tasks** tab
- Tap any task to see the details
- When you've recorded your part, tap the upload button to submit *(Phase 4 — coming soon)*

#### 6. Track your status
- **Pending** — task assigned, not submitted yet
- **Awaiting Approval** — recording submitted, waiting for leader review
- **Approved** — leader approved your recording ✓
- **Re-record** — leader wants you to redo it

---

## App Navigation

### Leader (6 tabs)
| Tab | Description |
|-----|-------------|
| 🏠 Home | Overview: next session, stats, task list, band progress |
| 🎵 Lineups | Create and manage song lineups with member assignments |
| 📅 Schedule | Weekly calendar of all band sessions |
| ✅ Tasks | Your personal task list |
| 🎸 Band | Band info, invite code, quick actions (sessions, tasks) |
| 👤 Profile | Edit profile, dark/light mode, sign out |

### Member (4 tabs)
| Tab | Description |
|-----|-------------|
| 🏠 Home | Overview: next session, your tasks, band progress |
| 📅 Schedule | Sessions you're part of |
| ✅ Tasks | Your assigned tasks with status |
| 👤 Profile | Edit profile, dark/light mode, sign out |

---

## Features

### ✅ Completed
- Register / login / OTP password reset (via email)
- Instrument selection per user
- Create band (leader) / join band by invite code (member)
- Role-based navigation (leader vs member)
- Lineup management: create, edit, delete — with YouTube song search
- Session scheduling: linked to lineup, date/time/duration picker
- Task management: leader assigns tasks per session to specific members
- Task status flow: pending → submitted → approved / rerecord
- Audio player for submitted recordings
- Weekly schedule calendar with infinite scroll
- Dark / light mode toggle
- Animated home screen with band progress bar

### 🔄 In Progress
| Phase | Feature |
|-------|---------|
| 4 | Recording submission (file picker + live mic recording + upload) |
| 4 | Home screen widget (OneUI) for quick task access |
| 5 | Leader review screen for submitted recordings |
| 6 | Band settings (edit name, kick members, transfer ownership) |

---

## Project Structure

```
app/src/main/java/com/example/hisync/
│
├── api/                    # Retrofit API interface + client
├── dto/                    # Data transfer objects
├── fragments/              # All UI fragments and bottom sheets
├── schedule/               # Week calendar fragments and adapter
├── model/                  # Local data models
│
├── MainActivity.java       # Navigation host (ViewPager2 + BottomNav)
├── SplashActivity.java     # Entry point + theme + routing
├── LoginActivity.java
├── RegisterActivity.java
├── BandSetupActivity.java
├── InstrumentSetupActivity.java
├── OnboardingActivity.java
├── EditProfileActivity.java
├── ForgotPasswordActivity.java
└── MusicNoteView.java      # Custom animated background view
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| App shows "Cannot connect to server" | Check that the backend is running and `SERVER_URL` in `build.gradle` matches your IP |
| App crashes on login | Make sure `InstrumentSetupActivity` is declared in `AndroidManifest.xml` |
| Schedule shows no sessions | Sessions only appear for your userId (member) or bandId (leader) — check that sessions exist in the DB |
| Dark mode doesn't apply on splash | Make sure `AppCompatDelegate.setDefaultNightMode()` is called before `super.onCreate()` in `SplashActivity` |
| Emulator can't reach backend | Use `10.0.2.2` as the IP instead of `localhost` or `127.0.0.1` |
