package pl.stapik.calendar.ui.connect

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import pl.stapik.calendar.data.config.ApiConfig
import pl.stapik.calendar.data.config.ApiConfigStorage
import pl.stapik.calendar.data.network.NetworkModule
import retrofit2.HttpException

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

    fun onBaseUrlChange(value: String) = _uiState.update { it.copy(baseUrl = value, testResult = null) }
    fun onApiKeyChange(value: String) = _uiState.update { it.copy(apiKey = value, testResult = null) }

    fun onSave() {
        val baseUrl = _uiState.value.baseUrl.trim()
        val apiKey = _uiState.value.apiKey.trim()
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, testResult = null) }
            runCatching {
                NetworkModule.createApi(baseUrl = baseUrl).getMe(apiKey = apiKey)
            }.onSuccess { me ->
                storage.save(ApiConfig(baseUrl = baseUrl, apiKey = apiKey))
                _uiState.update {
                    it.copy(
                        isTesting = false,
                        testResult = ConnectTestResult.Success(keyLabel = me.keyLabel, scope = me.scope)
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isTesting = false, testResult = ConnectTestResult.Error(mapConnectError(error)))
                }
            }
        }
    }

    private fun mapConnectError(error: Throwable): String = when (error) {
        is UnknownHostException, is SocketTimeoutException -> "No network connection"
        is HttpException -> when (error.code()) {
            401, 403 -> "Invalid API key"
            else -> "Server error (${error.code()})"
        }
        else -> error.message ?: "Unknown error"
    }
}

class ConnectViewModelFactory(private val storage: ApiConfigStorage) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ConnectViewModel(storage) as T
}