# Android Lab — MCA Coursework

Android development lab exercises from my **MCA at Jain (Deemed-to-be University)**, written in
**Kotlin** with **Jetpack Compose**.

Each lab is a self-contained Android Studio project covering one core concept.

## Labs

| # | Project | Concept |
|---|---|---|
| 01 | [`Lab-01-HelloWorld`](Lab-01-HelloWorld) | Project structure, Jetpack Compose basics, first composable |
| 02 | [`Lab02LifecycleDemo`](Lab02LifecycleDemo) | The Activity lifecycle — `onCreate` through `onDestroy`, state across configuration changes |
| 03 | [`Lab03FragmentDemo`](Lab03FragmentDemo) | Fragments, the fragment lifecycle, and fragment transactions |
| 04 | [`Lab04IntentDemo`](Lab04IntentDemo) | Explicit and implicit intents, passing data between activities |
| 05 | [`Lab05NotificationDemo`](Lab05NotificationDemo) | Notification channels, building and posting notifications |
| 06 | [`Lab06BasicViewsDemo`](Lab06BasicViewsDemo) | Basic Android Views — `EditText`, `RadioGroup`, `Spinner`, `CheckBox`, `SeekBar` |
| 07 | [`Lab07TourPlanner`](Lab07TourPlanner) | TourPlanner — Fragments, Intents, and notifications in a trip-planning flow |
| 08 | [`Lab08ListViewImageViewDemo`](Lab08ListViewImageViewDemo) | Adaptive UI with `ListView` and `ImageView` — single-pane on phones, two-pane on tablets |

## Stack

| | |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Build | Gradle (Kotlin DSL — `build.gradle.kts`) |
| IDE | Android Studio |

## Running a lab

Each folder is an independent project — open the specific lab directory in Android Studio rather
than the repository root:

```bash
# open, for example:
android-lab/Lab02LifecycleDemo
```

Then let Gradle sync and run on an emulator or device.

## About this repository

This is **coursework**, kept public for academic submission and verification. It's learning
material rather than production work.

For a production-oriented project, see
**[ecommerce-rest-api](https://github.com/thesunnycode/ecommerce-rest-api)** — a Spring Boot REST
API with JWT authentication, role-based authorization and Stripe payment integration.

---

**Sunny Kr Singh** · [thesunnycode.me](https://thesunnycode.me) ·
[GitHub](https://github.com/thesunnycode)
