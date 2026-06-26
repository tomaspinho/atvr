package com.tomaspinho.atvr.viewmodel.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tomaspinho.atvr.AtvApplication

class DeviceViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val app = AtvApplication.INSTANCE
        val credentialStorage = com.tomaspinho.atvr.data.CredentialStorage(app)
        val settingsRepository = com.tomaspinho.atvr.repository.SettingsRepository(app)
        val repository = com.tomaspinho.atvr.repository.DeviceRepository(credentialStorage)
        return DeviceViewModel(app.applicationContext, repository, settingsRepository, credentialStorage) as T
    }
}