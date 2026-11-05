package com.tylor.repwise.core

import com.tylor.repwise.core.Equipment.*
import com.tylor.repwise.core.Muscle.*

object ExerciseLibrary {
    val all: List<Exercise> = listOf(
        Exercise("bench_press", "Barbell Bench Press", CHEST, listOf(TRICEPS, SHOULDERS), BARBELL, true, SegmentKey.BENCH_PRESS),
        Exercise("db_bench_press", "Dumbbell Bench Press", CHEST, listOf(TRICEPS, SHOULDERS), DUMBBELL, true, SegmentKey.BENCH_PRESS),
        Exercise("incline_db_press", "Incline Dumbbell Press", CHEST, listOf(SHOULDERS, TRICEPS), DUMBBELL, true),
        Exercise("push_up", "Push-up", CHEST, listOf(TRICEPS, SHOULDERS), BODYWEIGHT, true),
        Exercise("cable_fly", "Cable Fly", CHEST, emptyList(), CABLE, false),
        Exercise("pec_deck", "Machine Chest Fly", CHEST, emptyList(), MACHINE, false),
        Exercise("deadlift", "Barbell Deadlift", BACK, listOf(HAMSTRINGS, GLUTES), BARBELL, true, SegmentKey.DEADLIFT),
        Exercise("barbell_row", "Barbell Row", BACK, listOf(BICEPS), BARBELL, true),
        Exercise("db_row", "One-arm Dumbbell Row", BACK, listOf(BICEPS), DUMBBELL, true),
        Exercise("pull_up", "Pull-up", BACK, listOf(BICEPS), BODYWEIGHT, true, SegmentKey.PULL_UP),
        Exercise("lat_pulldown", "Lat Pulldown", BACK, listOf(BICEPS), CABLE, true, SegmentKey.LAT_PULL_DOWN),
        Exercise("seated_cable_row", "Seated Cable Row", BACK, listOf(BICEPS), CABLE, true),
        Exercise("overhead_press", "Overhead Press", SHOULDERS, listOf(TRICEPS), BARBELL, true),
        Exercise("db_shoulder_press", "Dumbbell Shoulder Press", SHOULDERS, listOf(TRICEPS), DUMBBELL, true),
        Exercise("lateral_raise", "Dumbbell Lateral Raise", SHOULDERS, emptyList(), DUMBBELL, false),
        Exercise("face_pull", "Cable Face Pull", SHOULDERS, listOf(BACK), CABLE, false),
        Exercise("barbell_curl", "Barbell Curl", BICEPS, emptyList(), BARBELL, false),
        Exercise("db_curl", "Dumbbell Curl", BICEPS, emptyList(), DUMBBELL, false),
        Exercise("hammer_curl", "Hammer Curl", BICEPS, emptyList(), DUMBBELL, false),
        Exercise("cable_curl", "Cable Curl", BICEPS, emptyList(), CABLE, false),
        Exercise("tricep_pushdown", "Triceps Pushdown", TRICEPS, emptyList(), CABLE, false),
        Exercise("skull_crusher", "Skull Crusher", TRICEPS, emptyList(), BARBELL, false),
        Exercise("db_overhead_ext", "Overhead Dumbbell Extension", TRICEPS, emptyList(), DUMBBELL, false),
        Exercise("dip", "Dip", TRICEPS, listOf(CHEST, SHOULDERS), BODYWEIGHT, true),
        Exercise("back_squat", "Barbell Back Squat", QUADS, listOf(GLUTES, HAMSTRINGS), BARBELL, true, SegmentKey.SQUAT),
        Exercise("goblet_squat", "Goblet Squat", QUADS, listOf(GLUTES), DUMBBELL, true, SegmentKey.SQUAT),
        Exercise("leg_press", "Leg Press", QUADS, listOf(GLUTES), MACHINE, true, SegmentKey.LEG_PRESS),
        Exercise("leg_extension", "Leg Extension", QUADS, emptyList(), MACHINE, false, SegmentKey.LEG_EXTENSION),
        Exercise("db_lunge", "Dumbbell Lunge", QUADS, listOf(GLUTES, HAMSTRINGS), DUMBBELL, true, SegmentKey.LUNGE),
        Exercise("rdl", "Romanian Deadlift", HAMSTRINGS, listOf(GLUTES, BACK), BARBELL, true),
        Exercise("db_rdl", "Dumbbell Romanian Deadlift", HAMSTRINGS, listOf(GLUTES), DUMBBELL, true),
        Exercise("leg_curl", "Lying Leg Curl", HAMSTRINGS, emptyList(), MACHINE, false, SegmentKey.LEG_CURL),
        Exercise("hip_thrust", "Barbell Hip Thrust", GLUTES, listOf(HAMSTRINGS), BARBELL, true, SegmentKey.HIP_THRUST),
        Exercise("glute_bridge", "Glute Bridge", GLUTES, listOf(HAMSTRINGS), BODYWEIGHT, false, SegmentKey.HIP_THRUST),
        Exercise("standing_calf_raise", "Standing Calf Raise", CALVES, emptyList(), MACHINE, false),
        Exercise("db_calf_raise", "Dumbbell Calf Raise", CALVES, emptyList(), DUMBBELL, false),
        Exercise("plank", "Plank (reps = seconds)", CORE, emptyList(), BODYWEIGHT, false, SegmentKey.PLANK),
        Exercise("cable_crunch", "Cable Crunch", CORE, emptyList(), CABLE, false),
        Exercise("hanging_leg_raise", "Hanging Leg Raise", CORE, emptyList(), BODYWEIGHT, false),
    )

    private val byId = all.associateBy { it.id }
    fun get(id: String): Exercise? = byId[id]
}
