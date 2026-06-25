package com.tomaspinho.atvr.viewmodel.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tomaspinho.atvr.AtvApplication

class RemoteControlViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val app = AtvApplication.INSTANCE
        val credentialStorage = com.tomaspinho.atvr.data.CredentialStorage(app)
        val repository = com.tomaspinho.atvr.repository.DeviceRepository(credentialStorage)
        return RemoteControlViewModel(repository) as T
    }
}