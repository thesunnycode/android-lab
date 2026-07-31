# Lab 02 — Android Activity Lifecycle

## Aim
To implement and observe the Android Activity lifecycle by overriding its lifecycle callback methods (`onCreate`, `onStart`, `onResume`, `onPause`, `onStop`, `onRestart`, `onDestroy`), displaying each transition as a **Toast** on screen and logging it to **Logcat**.

## Concept / Technology Used
- **Android Studio** — IDE used to build and run the app
- **Kotlin** — programming language
- **Jetpack Compose** — used for the (static) UI layer
- **Activity Lifecycle** — the sequence of states (`Created → Started → Resumed → Paused → Stopped → Destroyed`) an Activity moves through as the user interacts with, backgrounds, or closes the app
- **Toast** — a short on-screen popup message (`Toast.makeText().show()`), used here to make visible transitions (`onCreate`, `onStart`, `onResume`) directly observable in the UI
- **Logcat** — Android's logging system, used via `Log.d()` as a reliable, complete record of every transition — including `onPause`, `onStop`, `onRestart`, and `onDestroy`, which may not always render a visible Toast since the app is no longer in the foreground at that point

## Scenario
This app demonstrates how an Activity's lifecycle methods fire in response to real user actions — launching the app, sending it to the background (Home button), bringing it back to the foreground (Recents), and closing it (Back button). A shared `logAndToast()` helper is called from every lifecycle method, so each transition is both shown as a Toast (when the app is visible) and always logged with tag `LifecycleDemo` (Logcat capturing every transition regardless of visibility).

## Folder Structure
```
Lab02LifecycleDemo/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/lab_02_lifecycledemo/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   └── ui/theme/
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   └── androidTest/
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── screenshots/
│   └── LIFECYCLE DEMO.mp4
└── README.md
```

## How to Run
1. Clone the repo and open `Lab02LifecycleDemo` in Android Studio
2. Let Gradle sync complete
3. Select a device/emulator from the device dropdown
4. Click **Run ▶**
5. Watch for **Toast** popups on screen during launch, background, and resume
6. Open **Logcat** (bottom toolbar) and filter by tag `LifecycleDemo` for the complete transition log, including transitions where no Toast is visible

## Demo Video

[▶ Watch the demo video](screenshots/LIFECYCLE%20DEMO.mp4) — click to view/download from the repo.

The video demonstrates, in sequence:
- App launch — `onCreate → onStart → onResume` (Log + Toast)
- App backgrounded and reopened — `onPause → onStop → onRestart → onStart → onResume`
- App closed via Back button — `onPause → onStop → onDestroy`
- The **customized Toast** widget displaying **Name & USN**
- **Logcat** (filtered by tag `LifecycleDemo`) confirming every transition in real time

## Conclusion
This experiment helped understand how an Android Activity transitions through its lifecycle states in response to user actions. Combining Toast (for immediate, visible feedback) with Logcat (for a complete, reliable record) showed that not every lifecycle callback is safely observable on screen — `onStop` and `onDestroy` fire after the app is no longer in the foreground, so Logcat remains the dependable source of truth for the full transition sequence.
