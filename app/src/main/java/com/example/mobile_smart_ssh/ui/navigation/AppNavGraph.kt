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
import com.example.mobile_smart_ssh.ui.terminal.TerminalActivity
import com.example.mobile_smart_ssh.ui.terminal.TerminalScreen
import com.example.mobile_smart_ssh.ui.wizard.SetupWizardScreen
import org.koin.compose.koinInject

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val repository: ConfigRepository = koinInject()

    // Single-Host Binding Model:
    // Jeśli host jest już skonfigurowany -> startuj bezpośrednio na Dashboardzie.
    // Jeśli brak powiązania -> uruchom Setup Wizard.
    val startDestination: Any = if (repository.isConfigured()) {
        DashboardRoute
    } else {
        SetupWizardRoute
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<SetupWizardRoute> {
            SetupWizardScreen(
                onFinished = {
                    navController.navigate(DashboardRoute) {
                        popUpTo<SetupWizardRoute> { inclusive = true }
                    }
                }
            )
        }

        composable<DashboardRoute> {
            val context = LocalContext.current
            DashboardScreen(
                onNavigateToTerminal = {
                    context.startActivity(TerminalActivity.createIntent(context))
                },
                onNavigateToFiles = { navController.navigate(FileBrowserRoute) },
                onNavigateToResources = { navController.navigate(ResourceMonitorRoute) },
                onNavigateToSettings = { navController.navigate(SettingsRoute) }
            )
        }

        composable<TerminalRoute> {
            TerminalScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<FileBrowserRoute> {
            FileBrowserScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<ResourceMonitorRoute> {
            ResourceMonitorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onResetToWizard = {
                    navController.navigate(SetupWizardRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
