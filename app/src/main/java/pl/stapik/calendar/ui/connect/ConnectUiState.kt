package pl.stapik.calendar.ui.connect

data class ConnectUiState(
    val baseUrl: String = "",
    val apiKey: String = "",
    val isTesting: Boolean = false,
    val testResult: ConnectTestResult? = null
)

sealed interface ConnectTestResult {
    data class Success(val keyLabel: String?, val scope: String) : ConnectTestResult
    data class Error(val message: String) : ConnectTestResult
}