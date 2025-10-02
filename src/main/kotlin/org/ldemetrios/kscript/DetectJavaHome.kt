package org.ldemetrios.kscript

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun detectJavaHome(): String? {
    return try {
        // Strategy 1: System property (most reliable for current JVM)
        System.getProperty("java.home")?.takeIf { isValidJavaHome(it) }
        // Strategy 2: JAVA_HOME environment variable
            ?: System.getenv("JAVA_HOME")?.takeIf { isValidJavaHome(it) }
            // Strategy 3: Try to find java executable and deduce home
            ?: findJavaHomeFromExecutable()
            // Strategy 4: Common installation directories
            ?: searchCommonJavaLocations()
    } catch (e: Exception) {
        null
    }
}

private fun isValidJavaHome(path: String): Boolean {
    val javaHome = File(path)
    if (!javaHome.exists() || !javaHome.isDirectory) return false

    // Check for key Java directories and files
    val hasBin = File(javaHome, "bin").exists()
    val hasJavaExec = File(javaHome, "bin/java").exists() ||
        File(javaHome, "bin/java.exe").exists()
    val hasJre = File(javaHome, "jre").exists()
    val hasLib = File(javaHome, "lib").exists()

    return hasBin && (hasJavaExec || hasJre || hasLib)
}

private fun findJavaHomeFromExecutable(): String? {
    return try {
        val javaExec = if (System.getProperty("os.name").lowercase().contains("windows")) {
            "java.exe"
        } else {
            "java"
        }

        // Try to run 'java -XshowSettings:properties -version' to get java.home
        val process = ProcessBuilder(javaExec, "-XshowSettings:properties", "-version")
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()

        // Parse output for java.home
        val javaHomeRegex = """java\.home\s*=\s*(.+)""".toRegex()
        javaHomeRegex.find(output)?.groupValues?.get(1)?.trim()
    } catch (e: Exception) {
        null
    }
}

private fun searchCommonJavaLocations(): String? {
    val commonPaths = mutableListOf<String>()

    // Platform-specific common locations
    when {
        System.getProperty("os.name").lowercase().contains("windows") -> {
            commonPaths.addAll(
                listOf(
                    "C:\\Program Files\\Java",
                    "C:\\Program Files (x86)\\Java"
                )
            )
            // Check each version subdirectory
            listOf("jdk", "jre").forEach { prefix ->
                (8..21).forEach { version ->
                    commonPaths.add("C:\\Program Files\\Java\\$prefix-$version")
                    commonPaths.add("C:\\Program Files\\Java\\$prefix.$version")
                }
            }
        }

        System.getProperty("os.name").lowercase().contains("mac") -> {
            commonPaths.add("/Library/Java/JavaVirtualMachines")
            commonPaths.add("/System/Library/Java/JavaVirtualMachines")
            commonPaths.add("/usr/local/opt/openjdk")

            // Check Homebrew locations
            val homebrewJava = "/usr/local/opt"
            File(homebrewJava).listFiles()?.forEach { file ->
                if (file.name.contains("openjdk") || file.name.contains("java")) {
                    commonPaths.add(file.absolutePath)
                }
            }
        }

        else -> { // Linux/Unix
            commonPaths.addAll(
                listOf(
                    "/usr/lib/jvm",
                    "/usr/java",
                    "/opt/java",
                    "/usr/local/java"
                )
            )

            // Check /usr/lib/jvm subdirectories
            File("/usr/lib/jvm").listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    commonPaths.add(file.absolutePath)
                }
            }
        }
    }

    return commonPaths.find { isValidJavaHome(it) }
}
