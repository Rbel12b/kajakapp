package com.rbel12b.kajakapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rbel12b.kajakapp.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(private val repo: SettingsRepository) : ViewModel() {

    private val _token = MutableStateFlow("")
    val token: StateFlow<String> = _token

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    init {
        viewModelScope.launch {
            _token.value = repo.tokenFlow.first()
        }
    }

    fun setToken(t: String) {
        _token.value = t
        _saved.value = false
    }

    fun save() {
        viewModelScope.launch {
            repo.saveToken(_token.value)
            _saved.value = true
        }
    }

    fun resetToDefault() {
        _token.value = SettingsRepository.DEFAULT_TOKEN
        _saved.value = false
    }

    companion object {
        fun factory(repo: SettingsRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(repo) as T
        }
    }
}
