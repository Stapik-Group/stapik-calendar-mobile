package pl.stapik.calendar.data.document

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class LineSetFile(private val file: Path) {
    fun load(): Set<String> =
        if (Files.exists(file)) Files.readAllLines(file).filter { it.isNotBlank() }.toSet() else emptySet()

    fun add(values: Collection<String>) {
        if (values.isNotEmpty()) write(load() + values)
    }

    fun remove(value: String) {
        val current = load()
        if (value in current) write(current - value)
    }

    private fun write(values: Set<String>) {
        Files.createDirectories(file.toAbsolutePath().parent)
        val temp = file.resolveSibling("${file.fileName}.tmp")
        Files.write(temp, values.sorted())
        Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE)
    }
}
