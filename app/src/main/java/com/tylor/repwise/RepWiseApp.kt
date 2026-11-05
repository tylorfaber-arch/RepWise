package com.tylor.repwise

import android.app.Application
import com.tylor.repwise.data.RepWiseDatabase
import com.tylor.repwise.health.HealthConnectManager

/** Simple manual dependency container; small enough that Hilt isn't worth it yet. */
class RepWiseApp : Application() {
    val database by lazy { RepWiseDatabase.create(this) }
    val healthConnect by lazy { HealthConnectManager(this) }
}
