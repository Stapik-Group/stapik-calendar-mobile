package pl.stapik.calendar.ui.calendar

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import pl.stapik.calendar.R
import pl.stapik.calendar.data.repository.CalendarNotFoundException
import pl.stapik.common.i18n.Messages
import pl.stapik.common.sync.CloudError
import pl.stapik.common.sync.CloudException
import pl.stapik.common.sync.UnreadableKind

sealed interface CalendarLoadError {
    data object NoNetwork : CalendarLoadError
    data object Unauthorized : CalendarLoadError
    data object NotFound : CalendarLoadError

    /** The cloud data cannot be read (encrypted, wrong passphrase, newer format); [messageKey] is a library message key. */
    data class Unreadable(val messageKey: String) : CalendarLoadError

    data class Unknown(val message: String) : CalendarLoadError
}

fun Throwable.toCalendarLoadError(): CalendarLoadError = when (this) {
    is CalendarNotFoundException -> CalendarLoadError.NotFound
    is CloudException -> when (val cloud = error) {
        CloudError.Unauthorized, CloudError.Forbidden -> CalendarLoadError.Unauthorized
        is CloudError.Network -> CalendarLoadError.NoNetwork
        is CloudError.Unreadable -> CalendarLoadError.Unreadable(unreadableKey(cloud.kind))
        is CloudError.ServerError -> CalendarLoadError.Unknown("HTTP ${cloud.httpCode}")
        is CloudError.RateLimited -> CalendarLoadError.Unknown("HTTP 429")
        CloudError.Conflict -> CalendarLoadError.Unknown("Conflict")
    }
    else -> CalendarLoadError.Unknown(message ?: this::class.simpleName ?: "Unknown error")
}

private fun unreadableKey(kind: UnreadableKind): String = when (kind) {
    UnreadableKind.PASSPHRASE_REQUIRED -> "sync.unreadable.passphrase_required"
    UnreadableKind.DECRYPTION_FAILED -> "sync.unreadable.wrong_passphrase"
    UnreadableKind.UNSUPPORTED_VERSION -> "sync.unreadable.newer_version"
    UnreadableKind.OTHER -> "sync.unreadable.other"
}

@Composable
fun CalendarLoadError.toDisplayMessage(): String = when (this) {
    CalendarLoadError.NoNetwork -> stringResource(R.string.error_no_network)
    CalendarLoadError.Unauthorized -> stringResource(R.string.error_unauthorized)
    CalendarLoadError.NotFound -> stringResource(R.string.error_not_found)
    is CalendarLoadError.Unreadable -> Messages.text(LocalContext.current, messageKey)
    is CalendarLoadError.Unknown -> stringResource(R.string.error_unknown, message)
}
