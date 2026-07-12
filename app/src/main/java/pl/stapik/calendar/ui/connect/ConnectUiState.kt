package pl.stapik.calendar.ui.connect

data class ConnectUiState(
    val baseUrl: String = "",
    val apiKey: String = "",
    val isSaved: Boolean = false
)