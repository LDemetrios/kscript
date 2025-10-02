package org.ldemetrios.kscript

import java.nio.file.Path
import kotlin.io.path.bufferedReader
import kotlin.io.path.inputStream

context(ctx: ApplicationContext) fun MutableList<ConfigEntry>.preprocess(
    origin: Path?,
    lines: List<String>,
    functions: MutableMap<String, FunctionDeclEntry>,
) {
    val parsed = parse(origin, lines)
    for (entry in parsed) when (entry) {
        is ClearEntry -> {
            debug { "[Preproc] Clearing" }
            this.clear()
        }

        is FileEntry -> {
            debug { "[Preproc] File $entry" }
            val resolved = if (entry.src.startsWith("/")) {
                Path.of(entry.src)
            } else {
                entry.origin!!.resolve(entry.src)
            }
            debug { "[Preproc] ... resolved to $resolved" }
            add(RawFileEntry(resolved.inputStream().readBytes(), entry.dst, entry.origin))
        }

        is FunctionCallEntry -> {
            debug { "[Preproc] Function call ${entry.name}(${entry.parameters.joinToString(", ")})" }
            val function = functions[entry.name] ?: error("Function ${entry.name} not found")
            val text = function
                .parameters
                .mapIndexed { idx, it -> "#$it" to entry.parameters[idx] }
                .fold(function.body) { acc, (key, value) -> acc.replace(key, value) }

            preprocess(function.origin, text.lines().toList(), functions)
        }

        is FunctionDeclEntry -> {
            debug { "[Preproc] Function declaration ${entry.name}" }
            functions[entry.name] = entry
        }

        is IncludeEntry -> {
            debug { "[Preproc] Include ${entry.path}" }
            val resolved = if (entry.path.startsWith("/")) {
                Path.of(entry.path)
            } else {
                entry.origin!!.resolve(entry.path)
            }
            debug { "[Preproc] Resolved to include $resolved" }
            preprocess(resolved, resolved.bufferedReader().readLines(), functions)
        }

        else -> {
            debug { "[Preproc] Boringly adding ${entry.javaClass}" }
            add(entry)
        }
    }
}