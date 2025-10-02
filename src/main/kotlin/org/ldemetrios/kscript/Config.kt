package org.ldemetrios.kscript

import java.nio.file.Path

sealed class ConfigEntry(open val origin: Path?)

data class DependencyEntry(
    val scope: DependencyScope,
    val group: String,
    val name: String,
    val version: String,
    override val origin: Path?,
) : ConfigEntry(origin)

sealed interface DependencyScope {
    val label: String

    enum class BuiltIn(override val label: String) : DependencyScope {
        IMPLEMENTATION("implementation"),
        API("api"),
        COMPILE("compileOnly"),
        RUNTIME("runtimeOnly"),
    }

    data class Custom(override val label: String) : DependencyScope
}

data class PluginEntry(
    val id: String,
    val version: String?,
    override val origin: Path?,
) : ConfigEntry(origin)

data class RepositoryEntry(
    val name: String,
    val url: String?,
    override val origin: Path?,
) : ConfigEntry(origin)

data class PluginRepositoryEntry(
    val name: String,
    val url: String?,
    override val origin: Path?,
) : ConfigEntry(origin)

data class IncludeEntry(
    val path: String,
    override val origin: Path?,
) : ConfigEntry(origin)

data class FileEntry(
    val src: String,
    val dst: String,
    override val origin: Path?,
) : ConfigEntry(origin)

data class RawInsertEntry(
    val body: String,
    val dst: String,
    override val origin: Path?,
) : ConfigEntry(origin)

data class RawFileEntry(
    val content: ByteArray,
    val dst: String,
    override val origin: Path?,
) : ConfigEntry(origin) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RawFileEntry

        if (!content.contentEquals(other.content)) return false
        if (dst != other.dst) return false
        if (origin != other.origin) return false

        return true
    }

    override fun hashCode(): Int {
        var result = content.contentHashCode()
        result = 31 * result + dst.hashCode()
        result = 31 * result + origin.hashCode()
        return result
    }
}

data class FunctionDeclEntry(
    val name: String,
    val parameters: List<String>,
    val body: String,
    override val origin: Path?,
) : ConfigEntry(origin)

data class FunctionCallEntry(
    val name: String,
    val parameters: List<String>,
    override val origin: Path?,
) : ConfigEntry(origin)

data class ClearEntry(
    override val origin: Path?,
) : ConfigEntry(origin)

data class OptionEntry(
    val name: String,
    val value: String,
    override val origin: Path?,
) : ConfigEntry(origin)
