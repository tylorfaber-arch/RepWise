# RepWise

A personal Android strength-training app with a calorie counter. It builds each workout around your most-recovered muscles, suggests weights for progressive overload, tracks a daily calorie budget, and writes workouts, food and weigh-ins to Health Connect.

## Open and run

1. Install Android Studio (latest stable) and open this folder.
2. Let Gradle sync. It downloads the Android SDK 35 and the libraries.
3. On your Pixel: Settings > About phone > tap Build number 7 times, then Developer options > USB debugging on.
4. Plug in the phone and press Run.
5. In the app, open **Me** > **Connect Health Connect** and allow the permissions.

## Layout

| Module | What's in it |
| --- | --- |
| `core` | Plain Kotlin, no Android: exercise library, recovery model, workout generator, progression, calorie math. Unit-tested (`./gradlew :core:test`). |
| `app` | Compose UI (Train, Food, Me tabs), Room database, Health Connect writes, WorkManager sync. |

## What syncs to Health Connect

| In RepWise | Health Connect record |
| --- | --- |
| Finished workout | `ExerciseSessionRecord` (strength training) with one segment per set |
| Workout calories (estimate) | `ActiveCaloriesBurnedRecord` |
| Food entry | `NutritionRecord` (calories, protein, carbs, fat, meal) |
| Weigh-in | `WeightRecord` |

Weights lifted per set stay in RepWise; Health Connect has no field for them.

## Known gaps in this first version

- The generated plan and the in-progress workout are kept in memory, so they're lost if Android kills the app mid-workout.
- No food database or barcode scanner yet; you type calories and macros (recent foods are one tap).
- Units are kg only.
