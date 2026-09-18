# Lab 07 — TourPlanner: Travel Itinerary

## Aim
To develop an Android application named **TourPlanner** that lets a user plan a trip by entering traveler details, choosing a destination category, viewing category details in a Fragment, adding the destination to an itinerary via an Intent, and receiving a notification on success — while demonstrating Activity and Fragment lifecycle methods through Logcat.

## Concept / Technology Used
- Android Studio
- Kotlin
- XML Views (no Jetpack Compose)
- `EditText` — Traveler Name, Destination, Number of Days
- `RadioGroup` / `RadioButton` — Adventure, Heritage, Relaxation category selection
- `Fragment` (`DestinationFragment`) — shows an image, suggested activities, a description, and an "Add to Trip" button, swapped in based on the selected category
- `FragmentManager` / `FragmentTransaction` — replaces the fragment container when a category is selected
- `Intent` — launches `ItineraryActivity` from `TripPlannerActivity` carrying the trip details as extras
- `NotificationChannel`, `NotificationCompat.Builder`, `NotificationManagerCompat` — posts a notification when a destination is successfully added to the itinerary
- Runtime permission `POST_NOTIFICATIONS` — required from Android 13 (API 33) onwards
- Activity/Fragment lifecycle callbacks (`onCreate`, `onStart`, `onResume`, `onPause`, `onStop`, `onDestroy`, `onAttach`, `onCreate`, `onViewCreated`, `onDestroyView`, `onDetach`) logged with `Log.d()` and verified in Logcat

## Scenario
`TripPlannerActivity` shows a form for **Traveler Name**, **Destination**, and **Number of Days**, followed by a `RadioGroup` of three destination categories — Adventure, Heritage, and Relaxation. Selecting a category loads `DestinationFragment` into a `FrameLayout` container, showing a category image, suggested activities, a short description, and an **Add to Trip** button. Tapping **Add to Trip** validates the form fields, then starts `ItineraryActivity` via an `Intent` carrying the traveler name, destination, days, category, and activities as extras, and posts a notification confirming the destination was added. `ItineraryActivity` displays the selected destination and its planned activities. Every lifecycle callback on both Activities and the Fragment logs a message via `Log.d()`, so the full lifecycle sequence — including the fragment swap and the transition between the two Activities — can be traced in Logcat.

## Wireframe

```
┌─────────────────────────────┐     ┌─────────────────────────────┐
│      Trip Planner            │     │      Trip Planner            │
│                               │     │                               │
│ Traveler Name  [___________] │     │ Traveler Name  [___________] │
│ Destination    [___________] │     │ Destination    [___________] │
│ No. of Days    [___________] │     │ No. of Days    [___________] │
│                               │     │                               │
│ Category:                    │     │ Category:                    │
│ ( ) Adventure                │     │ (•) Adventure                │
│ ( ) Heritage                 │     │ ( ) Heritage                 │
│ ( ) Relaxation               │     │ ( ) Relaxation               │
│                               │     │ ┌───────────────────────┐   │
│                               │     │ │ Destination Fragment   │   │
│                               │     │ │ [   image   ]          │   │
│                               │     │ │ Suggested Activities:  │   │
│                               │     │ │  - Trekking, Rafting   │   │
│                               │     │ │ Description: ......... │   │
│                               │     │ │    [ Add to Trip ]     │   │
│                               │     │ └───────────────────────┘   │
└─────────────────────────────┘     └─────────────────────────────┘
   TripPlannerActivity (initial)      TripPlannerActivity (fragment loaded)

┌─────────────────────────────┐
│        Itinerary              │
│                               │
│ Traveler: <name>              │
│ Destination: <destination>    │
│ Category: Adventure           │
│ Activities: Trekking, Rafting │
│                               │
│  [Notification: "Added to    │
│   Itinerary" shown in status  │
│   bar]                        │
└─────────────────────────────┘
      ItineraryActivity
```

## Folder Structure
```
LabTestTourPlanner/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/labtesttourplanner/
│       │   │   ├── TripPlannerActivity.kt
│       │   │   ├── ItineraryActivity.kt
│       │   │   └── DestinationFragment.kt
│       │   └── res/
│       │       ├── layout/
│       │       │   ├── activity_trip_planner.xml
│       │       │   ├── activity_itinerary.xml
│       │       │   └── fragment_destination.xml
│       │       ├── drawable/
│       │       │   ├── img_adventure.xml
│       │       │   ├── img_heritage.xml
│       │       │   ├── img_relaxation.xml
│       │       │   ├── ic_notification.xml
│       │       │   ├── ic_launcher_background.xml
│       │       │   └── ic_launcher_foreground.xml
│       │       └── values/
│       │           ├── colors.xml
│       │           ├── strings.xml
│       │           └── themes.xml
│       ├── androidTest/java/com/example/labtesttourplanner/ExampleInstrumentedTest.kt
│       └── test/java/com/example/labtesttourplanner/ExampleUnitTest.kt
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## How to Run
1. Clone this repository to your local machine.
2. Open Android Studio and select **Open**, then navigate to and select the `LabTestTourPlanner` folder.
3. Wait for Gradle to sync the project (Android Studio will do this automatically).
4. Select a target device — an emulator or a physical device connected via USB debugging.
5. Click **Run ▶** to build and launch the app.
6. Allow the notification permission prompt shown on first launch.
7. Enter a traveler name, destination, and number of days, then pick a category and tap **Add to Trip**.

## Conclusion
This experiment demonstrated how an Android application combines several core building blocks into a single flow: basic Views for data entry, a `Fragment` swapped dynamically based on a `RadioGroup` selection, an `Intent` carrying data between two Activities, and a notification confirming a user action. Logging every Activity and Fragment lifecycle callback and observing them in Logcat showed the precise order in which the system creates, starts, resumes, pauses, stops, and destroys these components as the user moves through the app.
