package com.tomaspinho.atvr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tomaspinho.atvr.repository.SettingsRepository
import com.tomaspinho.atvr.ui.screens.AtvRemoteScreen
import com.tomaspinho.atvr.ui.screens.OnboardingScreen
import com.tomaspinho.atvr.ui.theme.ATVRemoteTheme
import com.tomaspinho.atvr.viewmodel.onboarding.OnboardingViewModel
import com.tomaspinho.atvr.viewmodel.onboarding.OnboardingViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsRepository = SettingsRepository(this)
            val themeType by settingsRepository.themeFlow.collectAsStateWithLifecycle(
                initialValue = com.tomaspinho.atvr.ui.theme.ThemeType.SYSTEM
            )
            ATVRemoteTheme(themeType = themeType) {
                AppNavigation()
            }
        }
    }
}

private object Routes {
    const val ONBOARDING = "onboarding"
    const val REMOTE = "remote"
}

@Composable
private fun AppNavigation() {
    val navController = rememberNavController()
    val onboardingViewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModelFactory())
    val onboardingComplete by onboardingViewModel.onboardingComplete.collectAsStateWithLifecycle(initialValue = false)

    val startRoute = if (onboardingComplete) Routes.REMOTE else Routes.ONBOARDING

    NavHost(navController = navController, startDestination = startRoute) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                viewModel = onboardingViewModel,
                onComplete = {
                    onboardingViewModel.markOnboardingComplete()
                    navController.navigate(Routes.REMOTE) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.REMOTE) {
            var startWithDeviceSheetOpen by rememberSaveable { mutableStateOf(false) }
            AtvRemoteScreen(
                onResetOnboarding = {
                    onboardingViewModel.resetOnboarding()
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.REMOTE) { inclusive = true }
                    }
                },
                startWithDeviceSheetOpen = startWithDeviceSheetOpen,
                onStartWithDeviceSheetOpenConsumed = { startWithDeviceSheetOpen = false }
            )
        }
    }
}