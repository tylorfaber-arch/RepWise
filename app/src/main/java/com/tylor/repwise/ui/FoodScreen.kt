package com.tylor.repwise.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tylor.repwise.data.FoodEntry

private val MEALS = listOf("BREAKFAST", "LUNCH", "DINNER", "SNACK")

@Composable
fun FoodScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val budget by vm.dayBudget.collectAsStateWithLifecycle()
    val targets by vm.targets.collectAsStateWithLifecycle()
    val food by vm.todaysFood.collectAsStateWithLifecycle()
    val recent by vm.recentFoods.collectAsStateWithLifecycle()
    var addingTo by remember { mutableStateOf<String?>(null) }

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${budget.remainingKcal}", style = MaterialTheme.typography.displaySmall,
                        color = if (budget.remainingKcal < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    Text(if (budget.remainingKcal < 0) "Calories over" else "Calories remaining")
                    Text("Budget ${budget.budgetKcal}  -  Food ${budget.eatenKcal}  +  Exercise ${budget.exerciseKcal}",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    MacroRow("Protein", food.sumOf { it.proteinG }, targets.proteinG)
                    MacroRow("Carbs", food.sumOf { it.carbsG }, targets.carbsG)
                    MacroRow("Fat", food.sumOf { it.fatG }, targets.fatG)
                }
            }
        }
        MEALS.forEach { meal ->
            val entries = food.filter { it.meal == meal }
            item(key = meal) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${meal.lowercase().replaceFirstChar(Char::uppercase)}  ${entries.sumOf { it.kcal }} kcal",
                        style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { addingTo = meal }) { Text("Add food") }
                }
            }
            items(entries, key = { it.id }) { e ->
                Row(Modifier.fillMaxWidth().padding(start = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${e.name}  ·  ${e.kcal} kcal")
                    TextButton(onClick = { vm.deleteFood(e) }) { Text("Remove") }
                }
            }
        }
    }

    addingTo?.let { meal ->
        AddFoodDialog(meal, recent, onDismiss = { addingTo = null }, onAdd = { vm.addFood(it.copy(meal = meal)); addingTo = null })
    }
}

@Composable
private fun MacroRow(label: String, grams: Double, target: Int) {
    Column {
        Text("$label  ${grams.toInt()} / $target g", style = MaterialTheme.typography.bodySmall)
        LinearProgressIndicator(progress = { if (target > 0) (grams / target).toFloat().coerceIn(0f, 1f) else 0f }, Modifier.fillMaxWidth())
    }
}

@Composable
private fun AddFoodDialog(meal: String, recent: List<FoodEntry>, onDismiss: () -> Unit, onAdd: (FoodEntry) -> Unit) {
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    val number = KeyboardOptions(keyboardType = KeyboardType.Decimal)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to ${meal.lowercase()}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (recent.isNotEmpty()) {
                    Text("Recent", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(recent) { r -> AssistChip(onClick = { onAdd(r) }, label = { Text("${r.name} ${r.kcal}") }) }
                    }
                }
                OutlinedTextField(name, { name = it }, label = { Text("Food (blank = quick add)") }, singleLine = true)
                OutlinedTextField(kcal, { kcal = it }, label = { Text("Calories") }, keyboardOptions = number, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(protein, { protein = it }, Modifier.weight(1f), label = { Text("P g") }, keyboardOptions = number, singleLine = true)
                    OutlinedTextField(carbs, { carbs = it }, Modifier.weight(1f), label = { Text("C g") }, keyboardOptions = number, singleLine = true)
                    OutlinedTextField(fat, { fat = it }, Modifier.weight(1f), label = { Text("F g") }, keyboardOptions = number, singleLine = true)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val k = kcal.toDoubleOrNull()?.toInt() ?: return@TextButton
                onAdd(FoodEntry(name = name.ifBlank { "Quick add" }, kcal = k,
                    proteinG = protein.toDoubleOrNull() ?: 0.0, carbsG = carbs.toDoubleOrNull() ?: 0.0,
                    fatG = fat.toDoubleOrNull() ?: 0.0, meal = meal, eatenAt = 0))
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
