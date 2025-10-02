package org.ldemetrios.kscript

import java.nio.file.Path

fun String.parseStringLiteral() = this.trim()
    .removePrefix("\"")
    .removeSuffix("\"")
    .split("\\\\")
    .joinToString("\\") {
        it
            .replace("\\\"", "\"")
            .replace("\\t", "\t")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\b", "\b")
            .replace(Regex("\\\\u[0-9]{4}")) { it.value.toInt(16).toChar().toString() }
    }

context(ctx: ApplicationContext) fun parse(origin: Path?, lines: List<String>): List<ConfigEntry> = buildList {
    val iterator = lines.listIterator()
    while (iterator.hasNext()) {
        val line = iterator.next().trim()
        debug { "[Parser] Parsing line $line" }

        when {
            DependencyScope.BuiltIn
                .entries
                .toList()
                .any { line.startsWith(it.label) } -> {

                val label = line.takeWhile { it.isLetter() }
                val scope = DependencyScope.BuiltIn.entries.find { it.label == label }!!
                val (group, name, version) = line
                    .substringAfter("(")
                    .removeSuffix(")")
                    .parseStringLiteral()
                    .let { "$it:+" } // If no version, use latest
                    .split(":")

                add(DependencyEntry(scope, group, name, version, origin))
            }

            line.startsWith("dependency") -> {
                val args = line
                    .substringAfter("(")
                    .removeSuffix(")")
                    .trim()
                val scopePart = args
                    .takeWhile { it.isLetter() || it == '"' }
                val scope = DependencyScope.Custom(scopePart.filter { it.isLetter() })
                val (group, name, version) = args
                    .substringAfter(",")
                    .parseStringLiteral()
                    .let { "$it:+" } // If no version, use latest
                    .split(":")

                add(DependencyEntry(scope, group, name, version, origin))
            }

            line.startsWith("plugin") -> {
                val content = line
                    .substringAfter("(")
                    .removeSuffix(")")
                    .trim()

                val idPart = content.substringBefore(',')
                val version = content
                    .substringAfter(",")
                    .trim()
                    .takeIf { it.isNotEmpty() }

                val id = if (idPart.startsWith("\"")) {
                    idPart.parseStringLiteral()
                } else if (idPart.contains("(")) {
                    fatal { "[Parser] Function-like plugin declarations are not yet supported" }
                } else {
                    "org.gradle.$idPart"
                }

                add(PluginEntry(id, version, origin))
            }

            line.startsWith("repo") ||
                line.startsWith("pluginRepo") -> {
                val content = line.removePrefix("repo")
                    .substringAfter("(")
                    .removeSuffix(")")
                    .trim()

                val name = content.substringBefore(',')
                val url = content
                    .substringAfter(',')
                    .trim()
                    .takeIf { it.isNotEmpty() }

                if (line.startsWith("repo")) {
                    add(RepositoryEntry(name.trim(), url, origin))
                } else {
                    add(PluginRepositoryEntry(name.trim(), url, origin))
                }
            }

            line.startsWith("include") -> {
                val path = line
                    .substringAfter("(")
                    .removeSuffix(")")
                    .parseStringLiteral()

                add(IncludeEntry(path, origin))
            }

            line.startsWith("file") -> {
                val (src, dst) = line
                    .substringAfter("(")
                    .removeSuffix(")")
                    .parseStringLiteral()
                    .split(":")

                add(FileEntry(src, dst, origin))
            }

            line.startsWith("rawInsert") -> {
                val dst = line
                    .substringAfter("(")
                    .removeSuffix(")")
                    .parseStringLiteral()

                val content = mutableListOf<String>()
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    if (next.trimStart().startsWith("|")) {
                        content.add(next.substringAfter('|'))
                    } else {
                        iterator.previous()
                        break
                    }
                }

                add(RawInsertEntry(content.joinToString("\n"), dst, origin))
            }

            line.startsWith("rawFile") -> {
                val dst = line
                    .substringAfter("(")
                    .removeSuffix(")")
                    .parseStringLiteral()

                val content = mutableListOf<String>()
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    if (next.trimStart().startsWith("|")) {
                        content.add(next.substringAfter('|'))
                    } else {
                        iterator.previous()
                        break
                    }
                }

                add(RawFileEntry(content.joinToString("\n").toByteArray(), dst, origin))
            }

            line.startsWith("define") -> {
                val name = line.substringBefore("(").substringAfter("(")
                val argNames = line
                    .substringAfter("(")
                    .substringBefore(")")
                    .split(",")
                    .map { it.trim() }

                val body = mutableListOf<String>()
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    if (next.trimStart().startsWith("|")) {
                        body.add(next.substringAfter('|'))
                    } else {
                        iterator.previous()
                        break
                    }
                }

                add(FunctionDeclEntry(name, argNames, body.joinToString("\n"), origin))
            }

            line.startsWith("#") -> {
                val name = line.substringAfter("#").substringBefore("(")
                val args = line
                    .takeIf { it.contains("(") }
                    ?.substringAfter("(")
                    ?.substringBefore(")")
                    ?.split(",")
                    ?.map { it.trim() }

                add(FunctionCallEntry(name, args ?: listOf(), origin))
            }

            line == "clear" -> {
                add(ClearEntry(origin))
            }

            line.contains("=") -> {
                val (name, value) = line.split("=")
                add(OptionEntry(name.trim(), value.trim(), origin))
            }

            else -> {
                error("Unknown line type: $line")
            }
        }
    }
}