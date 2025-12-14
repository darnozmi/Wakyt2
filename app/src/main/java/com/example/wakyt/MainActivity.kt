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
import androidx.compose.runtime.collectAsState
import com.example.wakyt.ui.theme.WAKYTTheme
import com.example.wakyt.ui.HomeScreen
import com.example.wakyt.ui.TodayTasksScreen
import com.example.wakyt.ui.AddScreen
import com.example.wakyt.ui.SummaryScreen
import com.example.wakyt.data.AppRepository
import com.example.wakyt.ui.AccountScreen
import com.example.wakyt.ui.EditProfileScreen
import com.example.wakyt.ui.NotificationsSettingsScreen
import com.example.wakyt.ui.AppSettingsScreen
import com.example.wakyt.ui.PrivacyPolicyScreen
import com.example.wakyt.ui.FaqsScreen
import com.example.wakyt.ui.ChangePasswordScreen
import com.example.wakyt.ui.LoginScreen

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
                val isLoggedIn by AppRepository.isLoggedIn.collectAsState(initial = true)

                // Simple navigation stack for Account/Profile area
                var accountRoute by remember { mutableStateOf(AccountRoute.Hub) }

                if (!isLoggedIn) {
                    LoginScreen(onLogin = { AppRepository.setLoggedIn(true) })
                    return@WAKYTTheme
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = androidx.compose.ui.graphics.Color.White,
                    bottomBar = {
                        NavigationBar {
                            items.forEach { item ->
                                NavigationBarItem(
                                    selected = selectedTab == item,
                                    onClick = {
                                        selectedTab = item
                                        if (item == BottomNavItem.Profile) accountRoute = AccountRoute.Hub
                                    },
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
                                onOpenProfile = { selectedTab = BottomNavItem.Profile; accountRoute = AccountRoute.Hub },
                                onOpenSettings = { selectedTab = BottomNavItem.Profile; accountRoute = AccountRoute.Hub },
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
                            BottomNavItem.Profile -> when (accountRoute) {
                                AccountRoute.Hub -> AccountScreen(
                                    onBack = { selectedTab = BottomNavItem.Home },
                                    onOpenEditProfile = { accountRoute = AccountRoute.EditProfile },
                                    onOpenNotifications = { accountRoute = AccountRoute.Notifications },
                                    onOpenAppSettings = { accountRoute = AccountRoute.AppSettings },
                                    onOpenPrivacy = { accountRoute = AccountRoute.Privacy },
                                    onOpenFaq = { accountRoute = AccountRoute.Faqs },
                                    onOpenChangePassword = { accountRoute = AccountRoute.ChangePassword },
                                    onLogout = { AppRepository.setLoggedIn(false); selectedTab = BottomNavItem.Home }
                                )
                                AccountRoute.EditProfile -> EditProfileScreen(onBack = { accountRoute = AccountRoute.Hub })
                                AccountRoute.Notifications -> NotificationsSettingsScreen(onBack = { accountRoute = AccountRoute.Hub })
                                AccountRoute.AppSettings -> AppSettingsScreen(onBack = { accountRoute = AccountRoute.Hub })
                                AccountRoute.Privacy -> PrivacyPolicyScreen(onBack = { accountRoute = AccountRoute.Hub })
                                AccountRoute.Faqs -> FaqsScreen(onBack = { accountRoute = AccountRoute.Hub })
                                AccountRoute.ChangePassword -> ChangePasswordScreen(onBack = { accountRoute = AccountRoute.Hub })
                            }
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

private enum class AccountRoute { Hub, EditProfile, Notifications, AppSettings, Privacy, Faqs, ChangePassword }

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