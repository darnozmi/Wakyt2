package com.example.wakyt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.wakyt.ui.theme.WAKYTTheme
import com.example.wakyt.ui.HomeScreen
import com.example.wakyt.ui.TodayTasksScreen
import com.example.wakyt.ui.AddScreen
import com.example.wakyt.ui.SummaryScreen
import com.example.wakyt.data.AppRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize shared repository with persistence so data is loaded/saved across runs
        AppRepository.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            WAKYTTheme {
                var selectedTab by remember { mutableStateOf(BottomNavItem.Home) }
                val items = BottomNavItem.defaults

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = androidx.compose.ui.graphics.Color.White,
                    bottomBar = {
                        NavigationBar {
                            items.forEach { item ->
                                NavigationBarItem(
                                    selected = selectedTab == item,
                                    onClick = { selectedTab = item },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = null
                                )
                            }
                        }
                    }
                ) { padding ->
                    Surface(modifier = Modifier.fillMaxSize().padding(padding), color = androidx.compose.ui.graphics.Color.White) {
                        when (selectedTab) {
                            BottomNavItem.Home -> HomeScreen(
                                userName = "John Doe",
                                onOpenProfile = { selectedTab = BottomNavItem.Profile },
                                onOpenSettings = { selectedTab = BottomNavItem.Profile },
                                onViewTasks = { _ -> selectedTab = BottomNavItem.Today },
                                onAddTask = { _ -> selectedTab = BottomNavItem.Add },
                                onOpenTaskGroup = { _ -> selectedTab = BottomNavItem.Summary }
                            )
                            BottomNavItem.Today -> TodayTasksScreen(
                                onBack = { selectedTab = BottomNavItem.Home },
                                onOpenSettings = { /* TODO: open notifications/settings screen */ }
                            )
                            BottomNavItem.Add -> AddScreen()
                            BottomNavItem.Summary -> SummaryScreen()
                            BottomNavItem.Profile -> SimpleCenterTextScreen("Profile")
                        }
                    }
                }
            }
        }
    }
}

private enum class BottomNavItem(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home),
    Today("Today", Icons.Filled.ListAlt),
    Add("Add", Icons.Filled.AddCircle),
    Summary("Summary", Icons.Filled.Assessment),
    Profile("Profile", Icons.Filled.Person);

    companion object {
        val defaults = listOf(Home, Today, Add, Summary, Profile)
    }
}

@androidx.compose.runtime.Composable
private fun SimpleCenterTextScreen(text: String) {
    // Minimal placeholder screens; replace with real screens later.
    androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize()) {
        androidx.compose.material3.Text(
            text = text,
            modifier = Modifier
        )
    }
}