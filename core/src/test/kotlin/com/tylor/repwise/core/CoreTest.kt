package com.tylor.repwise.core

import org.junit.Assert.*
import org.junit.Test

class CoreTest {
    private val hour = 3_600_000L
    private val now = 1_000 * hour

    @Test fun recoveryDropsThenRefills() {
        val sets = List(4) { LoggedSet("bench_press", 8, 60.0, now) }
        val fresh = RecoveryCalculator.recovery(sets, now)
        assertEquals(52.0, fresh.getValue(Muscle.CHEST), 0.01)
        assertEquals(76.0, fresh.getValue(Muscle.TRICEPS), 0.01)
        val later = RecoveryCalculator.recovery(sets, now + 60 * hour)
        assertEquals(100.0, later.getValue(Muscle.CHEST), 0.01)
    }

    @Test fun progressionAddsWeightAtTopOfRange() {
        val sets = List(3) { LoggedSet("bench_press", 10, 60.0, now) }
        assertEquals(6 to 62.5, Progression.suggest(sets, 6..10))
        val short = listOf(LoggedSet("bench_press", 10, 60.0, now), LoggedSet("bench_press", 7, 60.0, now))
        assertEquals(8 to 60.0, Progression.suggest(short, 6..10))
        assertEquals(6 to null, Progression.suggest(emptyList(), 6..10))
    }

    @Test fun epley() = assertEquals(80.0, Progression.estimatedOneRepMax(60.0, 10), 0.01)

    @Test fun generatorAvoidsTiredMusclesAndRespectsEquipment() {
        val sets = List(5) { LoggedSet("bench_press", 8, 60.0, now) }
        val recovery = RecoveryCalculator.recovery(sets, now)
        val plan = WorkoutGenerator().generate(recovery, setOf(Equipment.DUMBBELL, Equipment.BODYWEIGHT), Goal.HYPERTROPHY, sets)
        assertTrue(plan.isNotEmpty())
        assertTrue(plan.none { it.exercise.primary == Muscle.CHEST })
        assertTrue(plan.all { it.exercise.equipment in setOf(Equipment.DUMBBELL, Equipment.BODYWEIGHT) })
        assertTrue(plan.first().exercise.compound)
    }

    @Test fun calorieTarget() {
        // 80 kg, 180 cm, 30 y male: BMR = 800 + 1125 - 150 + 5 = 1780
        assertEquals(1780.0, Calories.bmr(Sex.MALE, 80.0, 180.0, 30), 0.01)
        val t = Calories.dailyTarget(Sex.MALE, 80.0, 180.0, 30, ActivityLevel.MODERATE, WeightGoal.MAINTAIN)
        assertEquals(2759, t.kcal)
        assertEquals(160, t.proteinG)
        assertEquals(400, Calories.workoutKcal(80.0, 60.0))
        val lose = Calories.dailyTarget(Sex.MALE, 80.0, 180.0, 30, ActivityLevel.MODERATE, WeightGoal.LOSE_1)
        assertEquals(2259, lose.kcal)
        assertEquals(1500, Calories.dailyTarget(Sex.MALE, 60.0, 160.0, 60, ActivityLevel.SEDENTARY, WeightGoal.LOSE_2).kcal)
    }

    @Test fun budgetAndGoalDate() {
        assertEquals(700, DayBudget(2000, 1600, 300).remainingKcal)
        assertEquals(154, Calories.daysToGoal(90.0, 80.0, WeightGoal.LOSE_1))
        assertNull(Calories.daysToGoal(80.0, 90.0, WeightGoal.LOSE_1))
    }
}
