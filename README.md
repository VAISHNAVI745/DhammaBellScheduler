# DhammaBell Scheduler

Offline Android app for meditation centers running Vipassana / Children's
Anapana courses on Android TV boxes and mobiles. Computes every gong and
audio-track event for a course from a fixed Start Date, arms exact
`AlarmManager` alarms, and survives reboots/power cuts.

## Getting a build without installing Android Studio

1. Create a new **empty public or private GitHub repository**.
2. Upload every file/folder from this project into the repo root, preserving
   the folder structure exactly (`app/`, `.github/`, `build.gradle.kts`, etc.).
   Easiest way: `git init`, `git add .`, `git commit -m "initial commit"`,
   `git remote add origin <your repo url>`, `git push -u origin main`.
3. Go to the repo's **Actions** tab. The `Build DhammaBell Debug APK` workflow
   runs automatically on every push to `main`/`master`, or click
   **Run workflow** to trigger it manually.
4. When it finishes (green check), open the run, scroll to **Artifacts**, and
   download `app-debug`. Unzip it to get `app-debug.apk`.
5. Sideload the APK onto the TV box (via USB, a file manager app, or `adb
   install app-debug.apk`).

No local Gradle wrapper jar is committed to this repo — the workflow
provisions Gradle itself on the GitHub-hosted runner via
`gradle/actions/setup-gradle`, so there is nothing to pre-generate.

## First-run setup on the device

1. Launch the app. On Android 12+ it will prompt for the
   **"Alarms & reminders"** permission — grant it, or bells will not fire.
2. Copy your MP3s onto the device at:
   `/Android/data/org.dhamma.bell/files/DhammaAudio/`
   using the exact filenames referenced in `VipassanaTemplates.kt`
   (e.g. `morning_chant_day3.mp3`, `discourse_day7.mp3`) plus a
   `gong_single.mp3` fallback file. Any event whose file is missing at
   trigger time automatically falls back to `gong_single.mp3`, and if even
   that is missing, to a synthesized gong tone — it will never crash or go
   silent.
3. Open **New Course**, pick a template and a Start Date, and tap
   **Schedule & Arm Bells**.
4. Check **Alarm Queue** to see every future bell, and **Dashboard** for a
   live countdown to the next one plus a **Manual Test Gong** button.

## Architecture notes

- `data/VipassanaTemplates.kt` — hardcoded blueprints (day-offset + time-of-day
  only) for all four course types.
- `scheduler/AlarmScheduler.kt` — turns a template + Start Date into
  `ScheduledBellEntity` rows and arms `AlarmManager.setAlarmClock()` alarms.
- `scheduler/BellAlarmReceiver.kt` — receives the exact alarm, hands off to a
  foreground service for playback (a `BroadcastReceiver` alone is not
  reliable for multi-second audio playback).
- `scheduler/BootReceiver.kt` — re-arms every untriggered future alarm from
  Room after `BOOT_COMPLETED`/`QUICKBOOT_POWERON`/app update, since a reboot
  wipes AlarmManager's live registrations but not the database.
- `audio/BellPlaybackService.kt` — foreground service; holds a
  `PARTIAL_WAKE_LOCK` for the duration of playback, released in
  `MediaPlayer`'s `OnCompletionListener`.
- `audio/AudioPlaybackManager.kt` — fires a 200ms quiet priming tone before
  the real gong to wake sleeping HDMI DACs/amplifiers, then plays the
  resolved file (or a synthesized fallback tone if no file exists at all).
- `audio/AudioResolver.kt` — resolves requested filename → external
  `DhammaAudio/` folder → default gong → null (synthesized tone), logging
  every fallback.
