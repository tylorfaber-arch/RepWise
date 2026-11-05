package com.tylor.repwise.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tylor.repwise.core.*
import com.tylor.repwise.data.Profile

@Composable
fun MeScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val weighIns by vm.weighIns.collectAsStateWithLifecycle()
    val workouts by vm.finishedWorkouts.collectAsStateWithLifecycle()

    var hcGranted by remember { mutableStateOf(false) }
    val hcAvailable = remember { vm.healthConnect.isAvailable() }
    LaunchedEffect(Unit) { hcGranted = vm.healthConnect.hasAllPermissions() }
    val launcher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        hcGranted = granted.containsAll(vm.healthConnect.permissions)
        if (hcGranted) vm.syncNow()
    }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Health Connect
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Health Connect", style = MaterialTheme.typography.titleMedium)
                when {
                    !hcAvailable -> Text("Health Connect isn't available on this phone.")
                    hcGranted -> { Text("Connected. Workouts, food and weight sync automatically."); OutlinedButton(onClick = { vm.syncNow() }) { Text("Sync now") } }
                    else -> Button(onClick = { launcher.launch(vm.healthConnect.permissions) }) { Text("Connect Health Connect") }
                }
            }
        }

        // Weight
        var weightText by remember { mutableStateOf("") }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Weight", style = MaterialTheme.typography.titleMedium)
                val goal = WeightGoal.valueOf(profile.weightGoal)
                Text("Now ${fmt(profile.weightKg)} kg  ·  Goal ${fmt(profile.goalWeightKg)} kg")
                Calories.daysToGoal(profile.weightKg, profile.goalWeightKg, goal)?.let {
                    Text("About $it days to goal at ${goal.lbPerWeek} lb/week", style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(weightText, { weightText = it }, Modifier.width(120.dp), label = { Text("kg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    Button(onClick = { weightText.toDoubleOrNull()?.let { vm.addWeighIn(it); weightText = "" } }) { Text("Weigh in") }
                }
                weighIns.take(5).forEach { Text("${fmt(it.weightKg)} kg  ·  ${java.text.DateFormat.getDateInstance().format(it.measuredAt)}", style = MaterialTheme.typography.bodySmall) }
            }
        }

        ProfileEditor(profile, onSave = vm::saveProfile)

        Text("Workouts logged: ${workouts.size}", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ProfileEditor(profile: Profile, onSave: (Profile) -> Unit) {
    var p by remember(profile) { mutableStateOf(profile) }
    val number = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Profile", style = MaterialTheme.typography.titleMedium)
            Choice("Training goal", Goal.entries.map { it.name }, p.goal) { p = p.copy(goal = it) }
            Choice("Weight plan", WeightGoal.entries.map { it.name }, p.weightGoal) { p = p.copy(weightGoal = it) }
            Choice("Activity", ActivityLevel.entries.map { it.name }, p.activity) { p = p.copy(activity = it) }
            Choice("Sex (for calorie math)", Sex.entries.map { it.name }, p.sex) { p = p.copy(sex = it) }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NumField("Height cm", p.heightCm, number, Modifier.weight(1f)) { p = p.copy(heightCm = it) }
                NumField("Age", p.age.toDouble(), number, Modifier.weight(1f)) { p = p.copy(age = it.toInt()) }
                NumField("Goal kg", p.goalWeightKg, number, Modifier.weight(1f)) { p = p.copy(goalWeightKg = it) }
            }
            Text("Equipment", style = MaterialTheme.typography.labelMedium)
            val selected = p.equipmentSet()
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Equipment.entries.forEach { e ->
                    FilterChip(selected = e in selected, label = { Text(e.name.lowercase()) }, onClick = {
                        val next = if (e in selected) selected - e else selected + e
                        p = p.copy(equipment = next.joinToString(",") { it.name })
                    })
                }
            }
            Button(onClick = { onSave(p) }) { Text("Save") }
        }
    }
}

@Composable
private fun Choice(label: String, options: List<String>, value: String, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) { Text("$label: ${value.lowercase().replace('_', ' ')}") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o -> DropdownMenuItem(text = { Text(o.lowercase().replace('_', ' ')) }, onClick = { onPick(o); open = false }) }
        }
    }
}

@Composable
private fun NumField(label: String, value: Double, kb: KeyboardOptions, modifier: Modifier, onChange: (Double) -> Unit) {
    var text by remember(value) { mutableStateOf(fmt(value)) }
    OutlinedTextField(text, { text = it; it.toDoubleOrNull()?.let(onChange) }, modifier, label = { Text(label) }, keyboardOptions = kb, singleLine = true)
}
