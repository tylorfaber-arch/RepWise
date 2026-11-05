package com.tylor.repwise.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import com.tylor.repwise.core.Calories
import com.tylor.repwise.core.ExerciseLibrary
import com.tylor.repwise.core.SegmentKey
import com.tylor.repwise.data.FoodEntry
import com.tylor.repwise.data.SetLog
import com.tylor.repwise.data.WeighIn
import com.tylor.repwise.data.Workout
import java.time.Instant
import java.time.ZoneId

class HealthConnectManager(private val context: Context) {

    val permissions = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getWritePermission(NutritionRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
    )

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    suspend fun hasAllPermissions(): Boolean =
        isAvailable() && client.permissionController.getGrantedPermissions().containsAll(permissions)

    /**
     * Writes a finished workout as a strength-training session with one segment per set,
     * plus an estimated active-calories record. Re-sending the same workout overwrites the
     * earlier copy because the clientRecordId stays the same and the version goes up.
     */
    suspend fun writeWorkout(workout: Workout, sets: List<SetLog>, bodyWeightKg: Double) {
        val start = Instant.ofEpochMilli(workout.startedAt)
        val end = Instant.ofEpochMilli(workout.endedAt ?: return)
        val offset = ZoneId.systemDefault().rules.getOffset(start)
        val version = System.currentTimeMillis()

        val session = ExerciseSessionRecord(
            startTime = start,
            startZoneOffset = offset,
            endTime = end,
            endZoneOffset = offset,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
            title = workout.title,
            metadata = Metadata(clientRecordId = "workout-${workout.id}", clientRecordVersion = version),
            segments = segmentsFor(sets, start, end),
        )
        val minutes = (workout.endedAt - workout.startedAt) / 60_000.0
        val calories = ActiveCaloriesBurnedRecord(
            startTime = start,
            startZoneOffset = offset,
            endTime = end,
            endZoneOffset = offset,
            energy = Energy.kilocalories(Calories.workoutKcal(bodyWeightKg, minutes).toDouble()),
            metadata = Metadata(clientRecordId = "workout-kcal-${workout.id}", clientRecordVersion = version),
        )
        client.insertRecords(listOf(session, calories))
    }

    suspend fun writeFood(entry: FoodEntry) {
        val time = Instant.ofEpochMilli(entry.eatenAt)
        val offset = ZoneId.systemDefault().rules.getOffset(time)
        val record = NutritionRecord(
            startTime = time,
            startZoneOffset = offset,
            endTime = time.plusSeconds(60),
            endZoneOffset = offset,
            name = entry.name,
            energy = Energy.kilocalories(entry.kcal.toDouble()),
            protein = Mass.grams(entry.proteinG),
            totalCarbohydrate = Mass.grams(entry.carbsG),
            totalFat = Mass.grams(entry.fatG),
            mealType = when (entry.meal) {
                "BREAKFAST" -> MealType.MEAL_TYPE_BREAKFAST
                "LUNCH" -> MealType.MEAL_TYPE_LUNCH
                "DINNER" -> MealType.MEAL_TYPE_DINNER
                else -> MealType.MEAL_TYPE_SNACK
            },
            metadata = Metadata(clientRecordId = "food-${entry.id}", clientRecordVersion = System.currentTimeMillis()),
        )
        client.insertRecords(listOf(record))
    }

    suspend fun writeWeight(weighIn: WeighIn) {
        val time = Instant.ofEpochMilli(weighIn.measuredAt)
        client.insertRecords(listOf(WeightRecord(
            time = time,
            zoneOffset = ZoneId.systemDefault().rules.getOffset(time),
            weight = Mass.kilograms(weighIn.weightKg),
            metadata = Metadata(clientRecordId = "weight-${weighIn.id}", clientRecordVersion = System.currentTimeMillis()),
        )))
    }

    suspend fun deleteFood(entryId: Long) {
        if (!hasAllPermissions()) return
        client.deleteRecords(NutritionRecord::class, emptyList(), listOf("food-$entryId"))
    }

    /**
     * Health Connect wants segments inside the session and not overlapping. We don't know
     * exactly when a set started, so each set's segment runs from the previous set's finish
     * (or the session start) to its own completion time.
     */
    private fun segmentsFor(sets: List<SetLog>, start: Instant, end: Instant): List<ExerciseSegment> {
        var cursor = start
        return sets.sortedBy { it.completedAt }.mapNotNull { set ->
            val setEnd = Instant.ofEpochMilli(set.completedAt).coerceIn(start, end)
            if (!setEnd.isAfter(cursor)) return@mapNotNull null
            val segment = ExerciseSegment(
                startTime = cursor,
                endTime = setEnd,
                segmentType = segmentType(ExerciseLibrary.get(set.exerciseId)?.segment),
                repetitions = set.reps,
            )
            cursor = setEnd
            segment
        }
    }

    private fun segmentType(key: SegmentKey?): Int = when (key) {
        SegmentKey.BENCH_PRESS -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_BENCH_PRESS
        SegmentKey.SQUAT -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_SQUAT
        SegmentKey.DEADLIFT -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_DEADLIFT
        SegmentKey.LAT_PULL_DOWN -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_LAT_PULL_DOWN
        SegmentKey.LUNGE -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_LUNGE
        SegmentKey.LEG_PRESS -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_LEG_PRESS
        SegmentKey.LEG_CURL -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_LEG_CURL
        SegmentKey.LEG_EXTENSION -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_LEG_EXTENSION
        SegmentKey.PULL_UP -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_PULL_UP
        SegmentKey.PLANK -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK
        SegmentKey.HIP_THRUST -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_HIP_THRUST
        SegmentKey.WEIGHTLIFTING, null -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING
    }
}
