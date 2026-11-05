package com.tylor.repwise.core

import kotlin.math.roundToInt

/** Which units the person sees. Everything is stored metric (kg, cm, grams). */
enum class UnitSystem { IMPERIAL, METRIC }

object Units {
    const val LB_PER_KG = 2.2046226218
    const val CM_PER_IN = 2.54

    fun kgToLb(kg: Double) = kg * LB_PER_KG
    fun lbToKg(lb: Double) = lb / LB_PER_KG

    /** Body or lift weight shown in the person's units. */
    fun weightToDisplay(kg: Double, system: UnitSystem) = if (system == UnitSystem.IMPERIAL) kgToLb(kg) else kg
    fun weightFromDisplay(value: Double, system: UnitSystem) = if (system == UnitSystem.IMPERIAL) lbToKg(value) else value
    fun weightLabel(system: UnitSystem) = if (system == UnitSystem.IMPERIAL) "lb" else "kg"

    /** Height in total inches for imperial, cm for metric. */
    fun heightToDisplay(cm: Double, system: UnitSystem) = if (system == UnitSystem.IMPERIAL) cm / CM_PER_IN else cm
    fun heightFromDisplay(value: Double, system: UnitSystem) = if (system == UnitSystem.IMPERIAL) value * CM_PER_IN else value
    fun heightLabel(system: UnitSystem) = if (system == UnitSystem.IMPERIAL) "in" else "cm"

    /** 70.0 in -> "5' 10\"" */
    fun feetInches(cm: Double): String {
        val totalIn = (cm / CM_PER_IN).roundToInt()
        return "${totalIn / 12}' ${totalIn % 12}\""
    }
}

/**
 * Food amount units. Mass units convert exactly; volume units go through millilitres and
 * assume about 1 g per ml (right for drinks, rough for solids) unless the food's own
 * serving gives a better number. SERVING uses the food's serving size.
 */
enum class FoodUnit(val label: String, val grams: Double?, val ml: Double?) {
    SERVING("serving", null, null),
    GRAM("g", 1.0, null),
    OUNCE("oz", 28.349523125, null),
    POUND("lb", 453.59237, null),
    KILOGRAM("kg", 1000.0, null),
    MILLILITRE("ml", null, 1.0),
    LITRE("L", null, 1000.0),
    FLUID_OUNCE("fl oz", null, 29.5735295625),
    CUP("cup", null, 236.5882365),
    TABLESPOON("tbsp", null, 14.78676478125),
    TEASPOON("tsp", null, 4.92892159375),
}

/** A food's nutrition per 100 g (or 100 ml), plus its labelled serving size. */
data class FoodNutrition(
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val kcalPer100g: Double,
    val proteinPer100g: Double = 0.0,
    val carbsPer100g: Double = 0.0,
    val fatPer100g: Double = 0.0,
    val servingGrams: Double? = null,
    val servingLabel: String? = null,
    val source: String = "",
) {
    /** Grams for [amount] of [unit], or null when a serving is asked for but the food has none. */
    fun gramsFor(amount: Double, unit: FoodUnit): Double? = when {
        unit == FoodUnit.SERVING -> servingGrams?.let { it * amount }
        unit.grams != null -> unit.grams * amount
        else -> unit.ml!! * amount   // ~1 g/ml
    }

    fun macrosFor(amount: Double, unit: FoodUnit): ScaledMacros? {
        val g = gramsFor(amount, unit) ?: return null
        val f = g / 100.0
        return ScaledMacros(kcalPer100g * f, proteinPer100g * f, carbsPer100g * f, fatPer100g * f)
    }
}

data class ScaledMacros(val kcal: Double, val proteinG: Double, val carbsG: Double, val fatG: Double)
