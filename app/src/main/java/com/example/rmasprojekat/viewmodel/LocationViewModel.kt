package com.example.rmasprojekat.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LocationViewModel : ViewModel() {
    private val _isLocationEnabled = MutableLiveData(false)
    val isLocationEnabled: LiveData<Boolean> get() = _isLocationEnabled

    fun setLocationEnabled(enabled: Boolean) {
        _isLocationEnabled.value = enabled
    }
}
