package pl.stapik.calendar.ui.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.stapik.calendar.data.config.ApiConfig
import pl.stapik.calendar.data.config.ApiConfigStorage

class ConnectViewModel(private val storage: ApiConfigStorage) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectUiState())
    val uiState: StateFlow<ConnectUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            storage.load()?.let { config ->
                _uiState.update { it.copy(baseUrl = config.baseUrl, apiKey = config.apiKey) }
            }
        }
    }

    fun onBaseUrlChange(value: String) = _uiState.update { it.copy(baseUrl = value, isSaved = false) }
    fun onApiKeyChange(value: String) = _uiState.update { it.copy(apiKey = value, isSaved = false) }

    fun onSave() {
        viewModelScope.launch {
            storage.save(ApiConfig(baseUrl = _uiState.value.baseUrl.trim(), apiKey = _uiState.value.apiKey.trim()))
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}

class ConnectViewModelFactory(private val storage: ApiConfigStorage) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ConnectViewModel(storage) as T
}
