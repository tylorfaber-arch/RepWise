package com.tylor.repwise.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

enum class SyncStatus { PENDING, SYNCED, FAILED }

@Entity
data class Profile(
    @PrimaryKey val id: Int = 1,
    val goal: String = "HYPERTROPHY",          // core.Goal
    val equipment: String = "BARBELL,DUMBBELL,MACHINE,CABLE,BODYWEIGHT", // core.Equipment list
    val sex: String = "MALE",                   // core.Sex
    val weightKg: Double = 80.0,
    val heightCm: Double = 178.0,
    val age: Int = 30,
    val activity: String = "MODERATE",          // core.ActivityLevel
    val weightGoal: String = "MAINTAIN",        // core.WeightGoal
    val goalWeightKg: Double = 80.0,
    @ColumnInfo(defaultValue = "IMPERIAL") val units: String = "IMPERIAL", // core.UnitSystem
    @ColumnInfo(defaultValue = "") val usdaApiKey: String = "",   // optional free key from api.data.gov
)

@Entity(indices = [Index("measuredAt")])
data class WeighIn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weightKg: Double,
    val measuredAt: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
)

@Entity
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
)

@Entity(
    foreignKeys = [ForeignKey(Workout::class, ["id"], ["workoutId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("workoutId"), Index("exerciseId")],
)
data class SetLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseId: String,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double,
    val completedAt: Long,
)

@Entity(indices = [Index("eatenAt")])
data class FoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kcal: Int,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val meal: String = "SNACK",                // BREAKFAST, LUNCH, DINNER, SNACK
    @ColumnInfo(defaultValue = "1.0") val amount: Double = 1.0,
    @ColumnInfo(defaultValue = "SERVING") val unit: String = "SERVING", // core.FoodUnit
    val barcode: String? = null,
    val eatenAt: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
)

@Dao
interface ProfileDao {
    @Query("SELECT * FROM Profile WHERE id = 1") fun observe(): Flow<Profile?>
    @Query("SELECT * FROM Profile WHERE id = 1") suspend fun get(): Profile?
    @Upsert suspend fun save(profile: Profile)
}

@Dao
interface WorkoutDao {
    @Insert suspend fun insert(workout: Workout): Long
    @Update suspend fun update(workout: Workout)
    @Query("SELECT * FROM Workout WHERE id = :id") suspend fun get(id: Long): Workout?
    @Query("SELECT * FROM Workout WHERE endedAt IS NOT NULL ORDER BY startedAt DESC") fun observeFinished(): Flow<List<Workout>>
    @Query("SELECT * FROM Workout WHERE endedAt IS NOT NULL AND syncStatus != 'SYNCED'") suspend fun unsynced(): List<Workout>
    @Query("DELETE FROM Workout WHERE id = :id") suspend fun delete(id: Long)

    @Insert suspend fun insertSet(set: SetLog): Long
    @Query("DELETE FROM SetLog WHERE id = :id") suspend fun deleteSet(id: Long)
    @Query("SELECT * FROM SetLog WHERE workoutId = :workoutId ORDER BY completedAt") fun observeSets(workoutId: Long): Flow<List<SetLog>>
    @Query("SELECT * FROM SetLog WHERE workoutId = :workoutId ORDER BY completedAt") suspend fun sets(workoutId: Long): List<SetLog>
    @Query("SELECT * FROM SetLog WHERE completedAt >= :since") fun observeSetsSince(since: Long): Flow<List<SetLog>>
    @Query("SELECT * FROM SetLog") suspend fun allSets(): List<SetLog>
}

@Dao
interface FoodDao {
    @Insert suspend fun insert(entry: FoodEntry): Long
    @Update suspend fun update(entry: FoodEntry)
    @Query("DELETE FROM FoodEntry WHERE id = :id") suspend fun delete(id: Long)
    @Query("SELECT * FROM FoodEntry WHERE eatenAt BETWEEN :from AND :to ORDER BY eatenAt")
    fun observeBetween(from: Long, to: Long): Flow<List<FoodEntry>>
    /** Recent distinct foods, newest first, for one-tap re-logging. */
    @Query("SELECT * FROM FoodEntry WHERE id IN (SELECT MAX(id) FROM FoodEntry GROUP BY name) ORDER BY eatenAt DESC LIMIT 20")
    fun observeRecentFoods(): Flow<List<FoodEntry>>
    @Query("SELECT * FROM FoodEntry WHERE syncStatus != 'SYNCED'") suspend fun unsynced(): List<FoodEntry>
}

@Dao
interface WeighInDao {
    @Insert suspend fun insert(weighIn: WeighIn): Long
    @Update suspend fun update(weighIn: WeighIn)
    @Query("SELECT * FROM WeighIn ORDER BY measuredAt DESC") fun observeAll(): Flow<List<WeighIn>>
    @Query("SELECT * FROM WeighIn WHERE syncStatus != 'SYNCED'") suspend fun unsynced(): List<WeighIn>
}

@Database(entities = [Profile::class, Workout::class, SetLog::class, FoodEntry::class, WeighIn::class], version = 2, autoMigrations = [AutoMigration(from = 1, to = 2)])
abstract class RepWiseDatabase : RoomDatabase() {
    abstract fun profiles(): ProfileDao
    abstract fun workouts(): WorkoutDao
    abstract fun food(): FoodDao
    abstract fun weighIns(): WeighInDao

    companion object {
        fun create(context: Context): RepWiseDatabase =
            Room.databaseBuilder(context, RepWiseDatabase::class.java, "repwise.db").build()
    }
}
