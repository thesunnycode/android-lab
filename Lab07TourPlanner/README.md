# Lab 07 — TourPlanner: Travel Itinerary

## Aim
To develop an Android application named **TourPlanner** that lets a user plan a trip by entering traveler details, choosing a destination category, viewing category details in a Fragment, adding the destination to an itinerary via an Intent, and receiving a notification on success — while demonstrating Activity and Fragment lifecycle methods through Logcat.

## Components Required
- Android Studio
- Kotlin
- XML Views (no Jetpack Compose)
- `EditText` — Traveler Name, Destination, Number of Days
- `RadioButton` / `RadioGroup` (or `Button`s) — Adventure, Heritage, Relaxation category selection
- `Fragment` (`DestinationFragment`) — shows image, activities, description, and an "Add to Trip" button
- `FragmentManager` / `FragmentTransaction` — swaps the fragment content when a category is selected
- `Intent` — launches `ItineraryActivity` from `TripPlannerActivity`
- `NotificationChannel`, `NotificationCompat.Builder`, `NotificationManagerCompat` — posts a notification when a destination is added
- `ImageView` — destination image per category
- Activity/Fragment lifecycle callbacks (`onCreate`, `onStart`, `onResume`, `onPause`, `onStop`, `onDestroy`, `onCreateView`, `onViewCreated`, etc.) with `Log.d()` calls, verified in Logcat

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

## Project Structure

```
Lab07TourPlanner/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/example/lab07tourplanner/
│           │   ├── TripPlannerActivity.kt
│           │   ├── ItineraryActivity.kt
│           │   └── DestinationFragment.kt
│           └── res/
│               ├── layout/
│               │   ├── activity_trip_planner.xml
│               │   ├── activity_itinerary.xml
│               │   └── fragment_destination.xml
│               ├── drawable/
│               │   ├── img_adventure.xml
│               │   ├── img_heritage.xml
│               │   └── img_relaxation.xml
│               └── values/
│                   ├── colors.xml
│                   ├── strings.xml
│                   └── themes.xml
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```
