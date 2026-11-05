# RepWise

A personal Android strength-training app with a calorie counter. It builds each workout around your most-recovered muscles, suggests weights for progressive overload, tracks a daily calorie budget, and writes workouts, food and weigh-ins to Health Connect.

## Install on your phone (no cable)

1. On the Pixel, open the repo's **Releases** page and tap **RepWise.apk** under "RepWise (latest build)". Every push to `main` rebuilds it.
2. Open the download. The first time, Android asks you to allow installs from your browser or Files app; allow it and tap Install.
3. In the app, open **Me** > **Connect Health Connect** and allow the permissions.

Updates install over the old version and keep your data, because every build is signed with the same key (`app/debug.keystore`).

To build yourself: open the folder in Android Studio, or run `./gradlew :app:assembleRelease` (shrunk, about 3 MB).

## Food data

- **Barcode**: tap Add food > Scan barcode. It uses Google's on-device scanner (no camera permission needed) and looks the code up in Open Food Facts (3M+ products, free, no key), then USDA FoodData Central branded foods (~400k US products) if Open Food Facts doesn't have it.
- **Search**: searches USDA (including generic foods like "banana") and Open Food Facts together.
- **Units**: serving, g, oz, lb, kg, ml, L, fl oz, cup, tbsp, tsp. Volumes convert at about 1 g per ml.
- USDA works out of the box with a shared demo key that has a low rate limit. For your own limit, get a free key at https://api.data.gov/signup and paste it in **Me** > Profile.

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
- Body weight, lifts and height show in lb/in or kg/cm (Me > Profile > Units); everything is stored metric.
