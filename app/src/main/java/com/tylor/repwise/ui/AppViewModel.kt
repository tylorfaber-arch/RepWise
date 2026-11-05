package com.tylor.repwise.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tylor.repwise.RepWiseApp
import com.tylor.repwise.core.*
import com.tylor.repwise.data.*
import com.tylor.repwise.food.FoodLookup
import com.tylor.repwise.health.HealthSyncWorker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repwise = app as RepWiseApp
    private val db = repwise.database
    val healthConnect = repwise.healthConnect

    val profile: StateFlow<Profile> = db.profiles().observe().map { it ?: Profile() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Profile())

    // ---- Training ----

    private val recentSets = db.workouts().observeSetsSince(System.currentTimeMillis() - 5 * DAY)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recovery: StateFlow<Map<Muscle, Double>> = recentSets
        .map { sets -> RecoveryCalculator.recovery(sets.map { it.toCore() }, System.currentTimeMillis()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Muscle.entries.associateWith { 100.0 })

    private val _plan = MutableStateFlow<List<PlannedExercise>>(emptyList())
    val plan: StateFlow<List<PlannedExercise>> = _plan

    private val _activeWorkoutId = MutableStateFlow<Long?>(null)
    val activeWorkoutId: StateFlow<Long?> = _activeWorkoutId

    val activeSets: StateFlow<List<SetLog>> = _activeWorkoutId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else db.workouts().observeSets(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val finishedWorkouts = db.workouts().observeFinished()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun generateWorkout() = viewModelScope.launch {
        val p = profile.value
        val history = db.workouts().allSets().map { it.toCore() }
        val recent = recentSets.value.filter { System.currentTimeMillis() - it.completedAt < 3 * DAY }
            .map { it.exerciseId }.toSet()
        _plan.value = WorkoutGenerator().generate(
            recovery = recovery.value,
            equipment = p.equipmentSet(),
            goal = Goal.valueOf(p.goal),
            history = history,
            recentExerciseIds = recent,
        )
    }

    fun startWorkout() = viewModelScope.launch {
        val muscles = _plan.value.map { it.exercise.primary }.distinct()
            .joinToString(" & ") { it.name.lowercase().replaceFirstChar(Char::uppercase) }
        _activeWorkoutId.value = db.workouts().insert(
            Workout(title = muscles.ifBlank { "Workout" }, startedAt = System.currentTimeMillis())
        )
    }

    fun logSet(exerciseId: String, reps: Int, weightKg: Double) = viewModelScope.launch {
        val id = _activeWorkoutId.value ?: return@launch
        val n = activeSets.value.count { it.exerciseId == exerciseId } + 1
        db.workouts().insertSet(SetLog(workoutId = id, exerciseId = exerciseId, setNumber = n,
            reps = reps, weightKg = weightKg, completedAt = System.currentTimeMillis()))
    }

    fun deleteSet(setId: Long) = viewModelScope.launch { db.workouts().deleteSet(setId) }

    fun finishWorkout() = viewModelScope.launch {
        val id = _activeWorkoutId.value ?: return@launch
        val workout = db.workouts().get(id) ?: return@launch
        if (activeSets.value.isEmpty()) db.workouts().delete(id)
        else {
            db.workouts().update(workout.copy(endedAt = System.currentTimeMillis()))
            HealthSyncWorker.enqueue(getApplication())
        }
        _activeWorkoutId.value = null
        _plan.value = emptyList()
    }

    // ---- Food (calorie budget) ----

    private val today: Pair<Long, Long> get() {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
        return start to start + DAY - 1
    }

    val todaysFood: StateFlow<List<FoodEntry>> = db.food().observeBetween(today.first, today.second)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recentFoods = db.food().observeRecentFoods()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val targets: StateFlow<Macros> = profile.map { it.targets() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Profile().targets())

    val dayBudget: StateFlow<DayBudget> = combine(profile, todaysFood, finishedWorkouts) { p, food, workouts ->
        val (from, to) = today
        val exercise = workouts.filter { it.startedAt in from..to && it.endedAt != null }
            .sumOf { Calories.workoutKcal(p.weightKg, (it.endedAt!! - it.startedAt) / 60_000.0) }
        DayBudget(p.targets().kcal, food.sumOf { it.kcal }, exercise)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DayBudget(0, 0, 0))

    fun addFood(entry: FoodEntry) = viewModelScope.launch {
        db.food().insert(entry.copy(id = 0, eatenAt = System.currentTimeMillis(), syncStatus = SyncStatus.PENDING))
        HealthSyncWorker.enqueue(getApplication())
    }

    private val lookup = FoodLookup { profile.value.usdaApiKey.ifBlank { FoodLookup.DEMO_KEY } }

    /** Barcode or text lookup state for the add-food sheet. */
    private val _lookupBusy = MutableStateFlow(false)
    val lookupBusy: StateFlow<Boolean> = _lookupBusy
    private val _lookupMessage = MutableStateFlow<String?>(null)
    val lookupMessage: StateFlow<String?> = _lookupMessage
    private val _searchResults = MutableStateFlow<List<FoodNutrition>>(emptyList())
    val searchResults: StateFlow<List<FoodNutrition>> = _searchResults
    private val _scanned = MutableStateFlow<FoodNutrition?>(null)
    val scanned: StateFlow<FoodNutrition?> = _scanned

    fun lookupBarcode(code: String) = viewModelScope.launch {
        _lookupBusy.value = true; _lookupMessage.value = null
        val found = lookup.byBarcode(code)
        _scanned.value = found
        if (found == null) _lookupMessage.value = "No match for barcode $code. Enter it manually below."
        _lookupBusy.value = false
    }

    fun searchFood(query: String) = viewModelScope.launch {
        if (query.isBlank()) return@launch
        _lookupBusy.value = true; _lookupMessage.value = null
        _searchResults.value = lookup.search(query.trim())
        if (_searchResults.value.isEmpty()) _lookupMessage.value = "Nothing found (check your connection)."
        _lookupBusy.value = false
    }

    fun clearLookup() { _scanned.value = null; _searchResults.value = emptyList(); _lookupMessage.value = null }

    fun setLookupMessage(msg: String?) { _lookupMessage.value = msg }

    fun addPortion(food: FoodNutrition, amount: Double, unit: FoodUnit, meal: String) {
        val m = food.macrosFor(amount, unit) ?: return
        addFood(FoodEntry(
            name = listOfNotNull(food.brand, food.name).joinToString(" ").take(80),
            kcal = m.kcal.roundToInt(), proteinG = m.proteinG, carbsG = m.carbsG, fatG = m.fatG,
            meal = meal, amount = amount, unit = unit.name, barcode = food.barcode, eatenAt = 0,
        ))
    }

    fun deleteFood(entry: FoodEntry) = viewModelScope.launch {
        db.food().delete(entry.id)
        runCatching { healthConnect.deleteFood(entry.id) }
    }

    // ---- Weight & profile ----

    val weighIns = db.weighIns().observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun addWeighIn(kg: Double) = viewModelScope.launch {
        db.weighIns().insert(WeighIn(weightKg = kg, measuredAt = System.currentTimeMillis()))
        db.profiles().save(profile.value.copy(weightKg = kg))
        HealthSyncWorker.enqueue(getApplication())
    }

    fun saveProfile(p: Profile) = viewModelScope.launch { db.profiles().save(p) }

    fun syncNow() = HealthSyncWorker.enqueue(getApplication())

    companion object { const val DAY = 24 * 3_600_000L }
}

fun SetLog.toCore() = LoggedSet(exerciseId, reps, weightKg, completedAt)

fun Profile.unitSystem(): UnitSystem = runCatching { UnitSystem.valueOf(units) }.getOrDefault(UnitSystem.IMPERIAL)

fun Profile.equipmentSet(): Set<Equipment> =
    equipment.split(",").filter { it.isNotBlank() }.map { Equipment.valueOf(it) }.toSet()

fun Profile.targets(): Macros = Calories.dailyTarget(
    Sex.valueOf(sex), weightKg, heightCm, age, ActivityLevel.valueOf(activity), WeightGoal.valueOf(weightGoal)
)
