package com.example.mobile_smart_ssh.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mobile_smart_ssh.data.repository.ConfigRepository
import com.example.mobile_smart_ssh.ui.dashboard.DashboardScreen
import com.example.mobile_smart_ssh.ui.files.FileBrowserScreen
import com.example.mobile_smart_ssh.ui.resources.ResourceMonitorScreen
import com.example.mobile_smart_ssh.ui.settings.SettingsScreen
import com.example.mobile_smart_ssh.ui.terminal.TerminalScreen
import com.example.mobile_smart_ssh.ui.wizard.SetupWizardScreen

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val repository = ConfigRepository.getInstance(context)

    // Single-Host Binding Model:
    // Jeśli host jest już skonfigurowany -> startuj bezpośrednio na Dashboardzie.
    // Jeśli brak powiązania -> uruchom Setup Wizard.
    val startDestination = if (repository.isConfigured()) {
        Screen.Dashboard.route
    } else {
        Screen.SetupWizard.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.SetupWizard.route) {
            SetupWizardScreen(
                onFinished = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.SetupWizard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToTerminal = { navController.navigate(Screen.Terminal.route) },
                onNavigateToFiles = { navController.navigate(Screen.FileBrowser.route) },
                onNavigateToResources = { navController.navigate(Screen.ResourceMonitor.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Terminal.route) {
            TerminalScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.FileBrowser.route) {
            FileBrowserScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ResourceMonitor.route) {
            ResourceMonitorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onResetToWizard = {
                    navController.navigate(Screen.SetupWizard.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
