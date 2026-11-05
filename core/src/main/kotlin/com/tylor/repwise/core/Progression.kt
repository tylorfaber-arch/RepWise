package com.tylor.repwise.core

import kotlin.math.floor

object Progression {
    /** Epley estimated one-rep max. */
    fun estimatedOneRepMax(weightKg: Double, reps: Int): Double =
        if (reps <= 1) weightKg else weightKg * (1 + reps / 30.0)

    fun repRange(goal: Goal, compound: Boolean): IntRange = when (goal) {
        Goal.STRENGTH -> if (compound) 4..6 else 8..10
        Goal.HYPERTROPHY -> if (compound) 6..10 else 10..15
    }

    /**
     * Uses the most recent session's sets for one exercise. If every set reached the top of
     * the rep range, add one weight step; otherwise keep the weight and aim for one more rep.
     */
    fun suggest(
        lastSessionSets: List<LoggedSet>,
        range: IntRange,
        stepKg: Double = 2.5,
    ): Pair<Int, Double?> {
        if (lastSessionSets.isEmpty()) return range.first to null
        val weight = lastSessionSets.maxOf { it.weightKg }
        val atWeight = lastSessionSets.filter { it.weightKg == weight }
        val minReps = atWeight.minOf { it.reps }
        return if (minReps >= range.last) {
            range.first to roundToStep(weight + stepKg, stepKg)
        } else {
            (minReps + 1).coerceIn(range.first, range.last) to weight
        }
    }

    fun roundToStep(value: Double, step: Double): Double = floor(value / step + 0.5) * step
}
