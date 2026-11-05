package com.tylor.repwise.core

import kotlin.math.roundToInt

enum class Sex { MALE, FEMALE }

enum class ActivityLevel(val multiplier: Double) {
    SEDENTARY(1.2), LIGHT(1.375), MODERATE(1.55), VERY_ACTIVE(1.725)
}

/**
 * Weekly weight-change plans, like a calorie-budget app: 1 lb of body weight is about
 * 3,500 kcal, so each 1 lb/week works out to 500 kcal/day.
 */
enum class WeightGoal(val lbPerWeek: Double) {
    LOSE_2(-2.0), LOSE_1_5(-1.5), LOSE_1(-1.0), LOSE_0_5(-0.5), MAINTAIN(0.0), GAIN_0_5(0.5);
    val dailyKcalDelta: Int get() = (lbPerWeek * 500).roundToInt()
}

/** One day's budget view: what you've eaten, what exercise earned back, what's left. */
data class DayBudget(val budgetKcal: Int, val eatenKcal: Int, val exerciseKcal: Int) {
    val remainingKcal: Int get() = budgetKcal - eatenKcal + exerciseKcal
}

data class Macros(val kcal: Int, val proteinG: Int, val carbsG: Int, val fatG: Int)

object Calories {
    /** Mifflin-St Jeor basal metabolic rate, kcal/day. */
    fun bmr(sex: Sex, weightKg: Double, heightCm: Double, age: Int): Double {
        val base = 10 * weightKg + 6.25 * heightCm - 5 * age
        return if (sex == Sex.MALE) base + 5 else base - 161
    }

    fun dailyTarget(
        sex: Sex, weightKg: Double, heightCm: Double, age: Int,
        activity: ActivityLevel, goal: WeightGoal,
    ): Macros {
        val floor = if (sex == Sex.MALE) 1500 else 1200   // don't budget below a safe minimum
        val kcal = ((bmr(sex, weightKg, heightCm, age) * activity.multiplier).roundToInt() + goal.dailyKcalDelta)
            .coerceAtLeast(floor)
        val protein = (weightKg * 2.0).roundToInt()            // 2 g/kg for lifters
        val fat = (kcal * 0.25 / 9).roundToInt()               // 25% of calories
        val carbs = ((kcal - protein * 4 - fat * 9) / 4.0).roundToInt().coerceAtLeast(0)
        return Macros(kcal, protein, carbs, fat)
    }

    /** Days until [targetKg] at the plan's weekly rate, or null when maintaining or moving the wrong way. */
    fun daysToGoal(currentKg: Double, targetKg: Double, goal: WeightGoal): Int? {
        val kgPerWeek = goal.lbPerWeek * 0.4536
        val deltaKg = targetKg - currentKg
        if (kgPerWeek == 0.0 || deltaKg == 0.0 || (deltaKg < 0) != (kgPerWeek < 0)) return null
        return (deltaKg / kgPerWeek * 7).roundToInt()
    }

    /** Rough strength-training burn: MET 5.0 x body weight x hours. */
    fun workoutKcal(weightKg: Double, durationMinutes: Double, met: Double = 5.0): Int =
        (met * weightKg * durationMinutes / 60.0).roundToInt()
}
