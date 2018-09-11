package us.n8l.duplicatefinder

import java.io.FileInputStream
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.util.*

internal interface DupeFinderPath {
    val name: String
    val fullPath: String
    val parent: DirectoryPath?
}

internal class DirectoryPath(override val parent: DirectoryPath?,
                             override val fullPath: String,
                             override val name: String,
                             private val attrs: BasicFileAttributes?)
    : DupeFinderPath

internal class FilePath(
        override val parent: DirectoryPath?,
        path: Path,
        attrs: BasicFileAttributes,
        val entry: FileEntry
) : DupeFinderPath {
    override val name: String = path.fileName.toString()
    override val fullPath: String = path.normalize().toString()
    override fun toString(): String = fullPath
}

internal class FileEntry(path: Path, attrs: BasicFileAttributes, private val id: Int?) {
    val size: Long = attrs.size()
    val mtime: FileTime = attrs.lastModifiedTime()
    val ctime: FileTime = attrs.creationTime()
    val atime: FileTime = attrs.lastAccessTime()

    fun getId(): OptionalInt {
        return if (id != null) OptionalInt.of(id) else OptionalInt.empty()
    }
}
