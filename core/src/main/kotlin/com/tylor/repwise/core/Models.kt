package com.tylor.repwise.core

enum class Muscle(val recoveryHours: Double) {
    CHEST(60.0), BACK(60.0), SHOULDERS(48.0), BICEPS(48.0), TRICEPS(48.0),
    QUADS(72.0), HAMSTRINGS(72.0), GLUTES(72.0), CALVES(48.0), CORE(36.0)
}

enum class Equipment { BARBELL, DUMBBELL, MACHINE, CABLE, BODYWEIGHT }

/** Stable keys the app maps to Health Connect exercise segment constants. */
enum class SegmentKey {
    BENCH_PRESS, SQUAT, DEADLIFT, LAT_PULL_DOWN, LUNGE, LEG_PRESS, LEG_CURL,
    LEG_EXTENSION, PULL_UP, PLANK, HIP_THRUST, WEIGHTLIFTING
}

data class Exercise(
    val id: String,
    val name: String,
    val primary: Muscle,
    val secondary: List<Muscle> = emptyList(),
    val equipment: Equipment,
    val compound: Boolean,
    val segment: SegmentKey = SegmentKey.WEIGHTLIFTING,
)

data class LoggedSet(
    val exerciseId: String,
    val reps: Int,
    val weightKg: Double,
    val completedAtMillis: Long,
)

data class PlannedExercise(
    val exercise: Exercise,
    val sets: Int,
    val targetReps: Int,
    val suggestedWeightKg: Double?,
)

enum class Goal { STRENGTH, HYPERTROPHY }
