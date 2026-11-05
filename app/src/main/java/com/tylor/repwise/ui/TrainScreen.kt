package com.tylor.repwise.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tylor.repwise.core.PlannedExercise
import com.tylor.repwise.data.SetLog

@Composable
fun TrainScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val active by vm.activeWorkoutId.collectAsStateWithLifecycle()
    val plan by vm.plan.collectAsStateWithLifecycle()
    val recovery by vm.recovery.collectAsStateWithLifecycle()
    val sets by vm.activeSets.collectAsStateWithLifecycle()

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (active == null) {
            item { Text("Muscle recovery", style = MaterialTheme.typography.titleLarge) }
            items(recovery.entries.sortedByDescending { it.value }.toList()) { (muscle, pct) ->
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(muscle.name.lowercase().replaceFirstChar(Char::uppercase))
                        Text("${pct.toInt()}%")
                    }
                    LinearProgressIndicator(progress = { (pct / 100).toFloat() }, Modifier.fillMaxWidth())
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.generateWorkout() }) { Text(if (plan.isEmpty()) "Build today's workout" else "Regenerate") }
                    if (plan.isNotEmpty()) OutlinedButton(onClick = { vm.startWorkout() }) { Text("Start") }
                }
            }
            items(plan) { PlanCard(it) }
        } else {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Workout in progress", style = MaterialTheme.typography.titleLarge)
                    Button(onClick = { vm.finishWorkout() }) { Text("Finish") }
                }
            }
            items(plan) { planned ->
                ActiveExerciseCard(planned, sets.filter { it.exerciseId == planned.exercise.id },
                    onLog = { reps, kg -> vm.logSet(planned.exercise.id, reps, kg) },
                    onDelete = { vm.deleteSet(it) })
            }
        }
    }
}

@Composable
private fun PlanCard(p: PlannedExercise) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(p.exercise.name, style = MaterialTheme.typography.titleMedium)
            val weight = p.suggestedWeightKg?.let { " @ ${fmt(it)} kg" } ?: " (pick a starting weight)"
            Text("${p.sets} sets x ${p.targetReps} reps$weight")
        }
    }
}

@Composable
private fun ActiveExerciseCard(p: PlannedExercise, done: List<SetLog>, onLog: (Int, Double) -> Unit, onDelete: (Long) -> Unit) {
    var reps by remember { mutableStateOf(p.targetReps.toString()) }
    var kg by remember { mutableStateOf(p.suggestedWeightKg?.let(::fmt) ?: "") }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${p.exercise.name}  (${done.size}/${p.sets})", style = MaterialTheme.typography.titleMedium)
            done.forEach { s ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Set ${s.setNumber}: ${s.reps} x ${fmt(s.weightKg)} kg")
                    TextButton(onClick = { onDelete(s.id) }) { Text("Undo") }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(reps, { reps = it }, Modifier.width(80.dp), label = { Text("Reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(kg, { kg = it }, Modifier.width(100.dp), label = { Text("kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                Button(onClick = {
                    val r = reps.toIntOrNull(); val w = kg.toDoubleOrNull() ?: 0.0
                    if (r != null && r > 0) onLog(r, w)
                }) { Text("Log") }
            }
        }
    }
}

fun fmt(d: Double): String = if (d % 1.0 == 0.0) d.toInt().toString() else "%.1f".format(d)
