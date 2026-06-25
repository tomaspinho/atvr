package com.tomaspinho.atvr.viewmodel.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tomaspinho.atvr.AtvApplication

/**
 * Factory that wires OnboardingViewModel with the app's repositories.
 * Uses the application context to build CredentialStorage, SettingsRepository
 * and DeviceRepository.
 */
class OnboardingViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val app = AtvApplication.INSTANCE
        val credentialStorage = com.tomaspinho.atvr.data.CredentialStorage(app)
        val settingsRepository = com.tomaspinho.atvr.repository.SettingsRepository(app)
        val repository = com.tomaspinho.atvr.repository.DeviceRepository(credentialStorage)
        return OnboardingViewModel(repository, settingsRepository, credentialStorage) as T
    }
}