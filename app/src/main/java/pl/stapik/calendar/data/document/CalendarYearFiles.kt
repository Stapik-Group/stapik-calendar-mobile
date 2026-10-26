package pl.stapik.calendar.data.document

import java.nio.file.Files
import java.nio.file.Path
import pl.stapik.calendar.data.model.CalendarDocument
import pl.stapik.calendar.data.model.CalendarYears

class CalendarYearFiles(private val directory: Path) {
    private val prefix = "${CalendarDocument.slot.value}@"

    fun years(): Set<Int> {
        if (!Files.isDirectory(directory)) return emptySet()
        return Files.newDirectoryStream(directory, "$prefix*$SUFFIX").use { stream ->
            stream.mapNotNull { yearOf(it.fileName.toString()) }.toSet()
        }
    }

    private fun yearOf(fileName: String): Int? =
        CalendarYears.yearOfPartition(fileName.removePrefix(prefix).removeSuffix(SUFFIX))

    private companion object {
        const val SUFFIX = ".doc"
    }
}
