package com.example.mobile_smart_ssh.di

import com.example.mobile_smart_ssh.core.security.KeyManager
import com.example.mobile_smart_ssh.data.repository.ConfigRepository
import com.example.mobile_smart_ssh.monitor.ServerMonitor
import com.example.mobile_smart_ssh.network.ssh.SSHSessionManager
import com.example.mobile_smart_ssh.ui.dashboard.DashboardViewModel
import com.example.mobile_smart_ssh.ui.files.FileBrowserViewModel
import com.example.mobile_smart_ssh.ui.resources.ResourceMonitorViewModel
import com.example.mobile_smart_ssh.ui.settings.SettingsViewModel
import com.example.mobile_smart_ssh.ui.terminal.TerminalViewModel
import com.example.mobile_smart_ssh.ui.wizard.WizardViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Single instance of ConfigRepository managed by Koin
    single { ConfigRepository.getInstance(androidContext()) }
    single { KeyManager }
    // Global persistent SSH session manager (replicates desktop m_session behavior)
    single { SSHSessionManager(get()) }
    // Server monitor holding application context internally
    single { ServerMonitor(androidContext(), get()) }

    // ViewModels with constructor injection (ZERO Context injected into any ViewModel!)
    viewModel { DashboardViewModel(get(), get()) }
    viewModel { TerminalViewModel(get(), get()) }
    viewModel { ResourceMonitorViewModel(get()) }
    viewModel { FileBrowserViewModel(get()) }
    viewModel { SettingsViewModel(get(), get(), get()) }
    viewModel { WizardViewModel(get(), get()) }
}
