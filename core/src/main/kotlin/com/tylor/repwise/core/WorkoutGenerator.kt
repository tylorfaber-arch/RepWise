package com.tylor.repwise.core

class WorkoutGenerator(
    private val library: List<Exercise> = ExerciseLibrary.all,
) {
    /**
     * Picks the [muscleCount] most-recovered major muscles, then one compound and one
     * isolation exercise for each (as equipment allows), capped at [maxExercises].
     * Exercises done in [recentExerciseIds] are deprioritised so sessions rotate.
     */
    fun generate(
        recovery: Map<Muscle, Double>,
        equipment: Set<Equipment>,
        goal: Goal,
        history: List<LoggedSet>,
        recentExerciseIds: Set<String> = emptySet(),
        muscleCount: Int = 3,
        maxExercises: Int = 6,
    ): List<PlannedExercise> {
        val available = library.filter { it.equipment in equipment }
        val targets = recovery.entries
            .filter { it.key != Muscle.CORE && it.key != Muscle.CALVES }
            .filter { m -> available.any { it.primary == m.key } }
            .sortedByDescending { it.value }
            .take(muscleCount)
            .map { it.key }

        val picks = mutableListOf<Exercise>()
        for (muscle in targets) {
            val options = available.filter { it.primary == muscle }
                .sortedBy { if (it.id in recentExerciseIds) 1 else 0 }
            options.firstOrNull { it.compound }?.let(picks::add)
            options.firstOrNull { !it.compound }?.let(picks::add)
        }
        val ordered = picks.distinct().sortedByDescending { it.compound }.take(maxExercises)

        return ordered.map { ex ->
            val range = Progression.repRange(goal, ex.compound)
            val last = lastSession(history, ex.id)
            val (reps, weight) = Progression.suggest(last, range)
            PlannedExercise(ex, sets = if (ex.compound) 4 else 3, targetReps = reps, suggestedWeightKg = weight)
        }
    }

    /** Sets of [exerciseId] from the most recent day it was trained. */
    private fun lastSession(history: List<LoggedSet>, exerciseId: String): List<LoggedSet> {
        val sets = history.filter { it.exerciseId == exerciseId }
        val latest = sets.maxOfOrNull { it.completedAtMillis } ?: return emptyList()
        return sets.filter { latest - it.completedAtMillis < 12 * 3_600_000L }
    }
}
