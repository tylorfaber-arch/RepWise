package com.tylor.repwise.core

/**
 * Each muscle starts at 100%. A set drains [DRAIN_PER_SET] points from its primary
 * muscle and half that from each secondary muscle; muscles refill linearly over
 * their [Muscle.recoveryHours]. Numbers are starting guesses to tune.
 */
object RecoveryCalculator {
    const val DRAIN_PER_SET = 12.0

    fun recovery(
        sets: List<LoggedSet>,
        nowMillis: Long,
        lookup: (String) -> Exercise? = ExerciseLibrary::get,
    ): Map<Muscle, Double> {
        val fatigue = Muscle.entries.associateWith { 0.0 }.toMutableMap()
        for (set in sets.sortedBy { it.completedAtMillis }) {
            val ex = lookup(set.exerciseId) ?: continue
            val hoursAgo = (nowMillis - set.completedAtMillis) / 3_600_000.0
            if (hoursAgo < 0) continue
            fun apply(m: Muscle, amount: Double) {
                val remaining = amount * (1 - hoursAgo / m.recoveryHours)
                if (remaining > 0) fatigue[m] = fatigue.getValue(m) + remaining
            }
            apply(ex.primary, DRAIN_PER_SET)
            ex.secondary.forEach { apply(it, DRAIN_PER_SET / 2) }
        }
        return fatigue.mapValues { (_, f) -> (100.0 - f).coerceIn(0.0, 100.0) }
    }
}
