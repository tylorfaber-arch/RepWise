package com.tylor.repwise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tylor.repwise.ui.RepWiseTheme

/** Privacy note Health Connect links to from its permission screen. */
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RepWiseTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("How RepWise uses your health data", style = MaterialTheme.typography.headlineSmall)
                        Text("RepWise writes your workouts, estimated workout calories, the food you log and your weigh-ins to Health Connect so they appear in your other health apps.")
                        Text("RepWise does not read data from Health Connect, and nothing leaves your phone except through Health Connect.")
                    }
                }
            }
        }
    }
}
