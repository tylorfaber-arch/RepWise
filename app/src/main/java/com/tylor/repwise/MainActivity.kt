package com.tylor.repwise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tylor.repwise.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { RepWiseTheme { RepWiseRoot() } }
    }
}

private enum class Tab(val label: String) { TRAIN("Train"), FOOD("Food"), ME("Me") }

@Composable
private fun RepWiseRoot(vm: AppViewModel = viewModel()) {
    var tab by rememberSaveable { mutableStateOf(Tab.TRAIN) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        label = { Text(t.label) },
                        icon = {
                            Icon(
                                when (t) {
                                    Tab.TRAIN -> Icons.Default.FitnessCenter
                                    Tab.FOOD -> Icons.Default.Restaurant
                                    Tab.ME -> Icons.Default.Person
                                }, contentDescription = null
                            )
                        },
                    )
                }
            }
        }
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (tab) {
            Tab.TRAIN -> TrainScreen(vm, modifier)
            Tab.FOOD -> FoodScreen(vm, modifier)
            Tab.ME -> MeScreen(vm, modifier)
        }
    }
}
