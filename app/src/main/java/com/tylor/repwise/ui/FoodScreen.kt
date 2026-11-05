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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.tylor.repwise.core.FoodNutrition
import com.tylor.repwise.core.FoodUnit
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
                    val unitLabel = runCatching { FoodUnit.valueOf(e.unit).label }.getOrDefault(e.unit)
                    Column(Modifier.weight(1f)) {
                        Text(e.name)
                        Text("${fmt(e.amount)} $unitLabel  ·  ${e.kcal} kcal", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { vm.deleteFood(e) }) { Text("Remove") }
                }
            }
        }
    }

    addingTo?.let { meal ->
        val close = { addingTo = null; vm.clearLookup() }
        AddFoodDialog(vm, meal, recent, onDismiss = close, onAdd = { vm.addFood(it.copy(meal = meal)); close() },
            onAddPortion = { f, amt, unit -> vm.addPortion(f, amt, unit, meal); close() })
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
private fun AddFoodDialog(
    vm: AppViewModel, meal: String, recent: List<FoodEntry>,
    onDismiss: () -> Unit, onAdd: (FoodEntry) -> Unit, onAddPortion: (FoodNutrition, Double, FoodUnit) -> Unit,
) {
    val context = LocalContext.current
    val busy by vm.lookupBusy.collectAsStateWithLifecycle()
    val message by vm.lookupMessage.collectAsStateWithLifecycle()
    val results by vm.searchResults.collectAsStateWithLifecycle()
    val scanned by vm.scanned.collectAsStateWithLifecycle()
    var picked by remember { mutableStateOf<FoodNutrition?>(null) }
    LaunchedEffect(scanned) { scanned?.let { picked = it } }

    var query by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    val number = KeyboardOptions(keyboardType = KeyboardType.Decimal)

    val startScan = {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options).startScan()
            .addOnSuccessListener { b -> b.rawValue?.let(vm::lookupBarcode) }
            .addOnFailureListener { vm.setLookupMessage("Scanner unavailable: ${it.message}") }
        Unit
    }

    picked?.let { food ->
        PortionDialog(food, onBack = { picked = null; vm.clearLookup() }, onAdd = { amt, unit -> onAddPortion(food, amt, unit) })
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to ${meal.lowercase()}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = startScan, Modifier.fillMaxWidth()) { Text("Scan barcode") }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(query, { query = it }, Modifier.weight(1f), label = { Text("Search foods") }, singleLine = true)
                    TextButton(onClick = { vm.searchFood(query) }) { Text("Go") }
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                results.take(25).forEach { r ->
                    Column(Modifier.fillMaxWidth().clickable { picked = r }.padding(vertical = 4.dp)) {
                        Text(listOfNotNull(r.brand, r.name).joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
                        Text("${r.kcalPer100g.toInt()} kcal / 100 g · ${r.source}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (recent.isNotEmpty()) {
                    Text("Recent", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(recent) { r -> AssistChip(onClick = { onAdd(r) }, label = { Text("${r.name} ${r.kcal}") }) }
                    }
                }
                Text("Or enter it yourself", style = MaterialTheme.typography.labelMedium)
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

/** Pick how much: amount + unit (serving, g, oz, cup, tbsp...) with live calories and macros. */
@Composable
private fun PortionDialog(food: FoodNutrition, onBack: () -> Unit, onAdd: (Double, FoodUnit) -> Unit) {
    val units = FoodUnit.entries.filter { it != FoodUnit.SERVING || food.servingGrams != null }
    var unit by remember { mutableStateOf(units.first()) }
    var amountText by remember { mutableStateOf(if (unit == FoodUnit.GRAM) "100" else "1") }
    var open by remember { mutableStateOf(false) }
    val amount = amountText.toDoubleOrNull() ?: 0.0
    val m = food.macrosFor(amount, unit)

    AlertDialog(
        onDismissRequest = onBack,
        title = { Text(listOfNotNull(food.brand, food.name).joinToString(" · ")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                food.servingLabel?.let { Text("Serving: $it", style = MaterialTheme.typography.bodySmall) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(amountText, { amountText = it }, Modifier.width(100.dp), label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    Box {
                        OutlinedButton(onClick = { open = true }) { Text(unit.label) }
                        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                            units.forEach { u -> DropdownMenuItem(text = { Text(u.label) }, onClick = { unit = u; open = false }) }
                        }
                    }
                }
                if (m != null) {
                    Text("${m.kcal.toInt()} kcal", style = MaterialTheme.typography.headlineSmall)
                    Text("Protein ${fmt(m.proteinG)} g · Carbs ${fmt(m.carbsG)} g · Fat ${fmt(m.fatG)} g", style = MaterialTheme.typography.bodySmall)
                }
                if (unit.ml != null) Text("Volume is converted at about 1 g per ml.", style = MaterialTheme.typography.bodySmall)
                Text("Source: ${food.source}", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(enabled = m != null && amount > 0, onClick = { onAdd(amount, unit) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onBack) { Text("Back") } },
    )
}
