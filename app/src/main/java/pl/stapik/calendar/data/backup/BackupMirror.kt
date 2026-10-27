package pl.stapik.calendar.data.backup

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import pl.stapik.calendar.data.model.CalendarYears

class BackupMirror(private val directory: Path) {
    fun years(): Set<Int> {
        if (!Files.isDirectory(directory)) return emptySet()
        return Files.newDirectoryStream(directory, "$PREFIX*$SUFFIX").use { stream ->
            stream.mapNotNull { yearOf(it.fileName.toString()) }.toSet()
        }
    }

    fun has(year: Int): Boolean = Files.exists(fileOf(year))

    fun read(year: Int): String? =
        fileOf(year).takeIf { Files.exists(it) }?.let { Files.readAllBytes(it).toString(Charsets.UTF_8) }

    fun write(year: Int, content: String) {
        Files.createDirectories(directory)
        val file = fileOf(year)
        val temp = directory.resolve("${file.fileName}$TEMP_SUFFIX")
        Files.write(temp, content.toByteArray(Charsets.UTF_8))
        Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE)
    }

    fun delete(year: Int) {
        Files.deleteIfExists(fileOf(year))
    }

    private fun fileOf(year: Int): Path = directory.resolve("$PREFIX${CalendarYears.partitionOf(year)}$SUFFIX")

    private fun yearOf(fileName: String): Int? =
        CalendarYears.yearOfPartition(fileName.removePrefix(PREFIX).removeSuffix(SUFFIX))

    private companion object {
        const val PREFIX = "calendar@"
        const val SUFFIX = ".json"
        const val TEMP_SUFFIX = ".tmp"
    }
}
