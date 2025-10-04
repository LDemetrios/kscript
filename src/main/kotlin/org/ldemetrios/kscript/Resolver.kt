package org.ldemetrios.kscript

import java.io.File
import java.util.Optional

context(ctx: ApplicationContext) fun resolve(config: List<ConfigEntry>, sourceFile: String): Map<String, ByteArray> {
    val plugins = mutableMapOf<String, Optional<String>>()
    val dependencies = mutableMapOf<DependencyEntry, String>()
    val rawFiles = mutableMapOf<String, ByteArray>()
    val rawInserts = mutableMapOf<String, MutableList<String>>()
    val options = mutableMapOf<String, String>()
    val repositories = mutableListOf<String>()
    val pluginRepos = mutableListOf<String>()

    val kotlinVersion = config
        .filterIsInstance<OptionEntry>()
        .lastOrNull { it.name == "kotlin.version" }
        ?.value

    val kotlinJvmPlugin = listOf(PluginEntry("org.jetbrains.kotlin.jvm", kotlinVersion ?: "2.2.0", null))
    val config = if (kotlinVersion == null) {
        kotlinJvmPlugin + config
    } else {
        config + kotlinJvmPlugin
    }

    for (entry in config) when (entry) {
        is PluginEntry -> {
            debug { "[Resolver] Plugin ${entry.id} ${entry.version}" }
            if (rawFiles.containsKey("build.gradle.kts")) {
                fatal { "Can't add plugin to manually overridden build file" }
            }
            when (entry.id) {
                "org.gradle.application" /*, "com.gradleup.shadow" */ -> error { "Overriding ${entry.id} may ruin your build process" }
            }
            val was = plugins[entry.id]
            when {
                was == null || was.isEmpty -> {
                    plugins[entry.id] = Optional.ofNullable(entry.version)
                }

                entry.version == null -> Unit // Don't update
                else -> {
                    warn { "Overriding plugin ${entry.id} from ${was.get()} to ${entry.version}" }
                }
            }
        }

        is DependencyEntry -> {
            val versionless = entry.copy(version = "")
            if (versionless in dependencies) {
                warn { "Overriding dependency $versionless from ${dependencies[versionless]} to ${entry.version}" }
            }
            dependencies[versionless] = entry.version
        }

        is RawFileEntry -> {
            val dst = resolvePath(entry.dst)
            when (dst) {
                "build.gradle.kts" -> {
                    warn { "Overriding build file removes plugins, repositories, dependencies and some of the options" }
                    plugins.clear()
                    dependencies.clear()
                    repositories.clear()
                    // TODO Options
                }

                "build.gradle" -> error { "KScript supports only Kotlin DSL, overriding `build.gradle` is useless at best" }
                "settings.gradle.kts" -> {
                    warn { "Overriding settings file removes plugin  repositories and some of the options" }
                    pluginRepos.clear()
                    // TODO Options
                }

                "settings.gradle" -> error { "KScript supports only Kotlin DSL, overriding `settings.gradle` is useless at best" }
                "gradle.properties" -> {
                    warn { "Overriding gradle.properties removes some of the options" }
                    // TODO Options
                }

                in listOf("gradlew", "gradlew.bat") -> error { "Overriding gradle wrapper may ruin your build" }
                "gradle/wrapper/gradle-wrapper.jar" -> fatal { "Gradle wrapper jar is a binary file, overriding is not permitted" }
                "gradle/wrapper/gradle-wrapper.properties" -> {
                    warn { "Overriding gradle wrapper properties removes some of the options" }
                    // TODO Options
                }

                in rawFiles -> warn { "Overriding file $dst" }
            }
            rawFiles[entry.dst] = entry.content
            if (rawInserts.remove(entry.dst) != null) {
                warn { "Overriding file $dst inserts" }
            }
        }

        is RawInsertEntry -> {
            rawInserts.computeIfAbsent(resolvePath(entry.dst)) { mutableListOf() }.add(entry.body)
        }

        is OptionEntry -> {
            if (entry.name in options) {
                warn { "Overriding option ${entry.name}" }
            }
            options[entry.name] = entry.value
        }

        is RepositoryEntry -> {
            if (entry.url != null) {
                repositories.add("${entry.name}(${entry.url})")
            } else {
                repositories.add(entry.name + "()")
            }
        }

        is PluginRepositoryEntry -> {
            if (entry.url != null) {
                pluginRepos.add("${entry.name}(${entry.url})")
            } else {
                pluginRepos.add(entry.name + "()")
            }
        }

        else -> {
            throw IllegalArgumentException("Entry $entry left unprocessed")
        }
    }

    fun maybeFile(path: String, func: StringBuilder.() -> Unit) =
        rawFiles[path]
            ?.let { StringBuilder(String(it)) } ?: StringBuilder().apply { func() }

    val buildFile = maybeFile("build.gradle.kts") {
        append("plugins {\n")
        for ((id, version) in plugins) {
            append("    id(\"$id\")")
            if (!version.isEmpty) append(" version \"${version.get()}\"")
            append("\n")
        }
        if ("org.gradle.application" !in plugins) append("    application\n")
        // if ("com.gradleup.shadow" !in plugins) append("    id(\"com.gradleup.shadow\")\n")
        append("}\n")
    }

    val settingsFile = maybeFile("settings.gradle.kts") {
        append("rootProject.name = \"kscript-generated-project\"\n")
    }

    val propertiesFile = maybeFile("gradle.properties") { }

    val gradleWrapperProperties = maybeFile("gradle/wrapper/gradle-wrapper.properties") {}

    val loader = ApplicationContext::class.java.classLoader

    rawFiles.computeIfAbsent("gradlew") { loader.getResourceAsStream("gradle-init/gradlew")!!.readAllBytes() }
    rawFiles.computeIfAbsent("gradlew.bat") { loader.getResourceAsStream("gradle-init/gradlew.bat")!!.readAllBytes() }

    buildFile.append("repositories {\n")
    buildFile.append("    mavenCentral()\n")
    for (repo in repositories) {
        buildFile.append("    $repo\n")
    }
    buildFile.append("}\n\ndependencies {")
    for ((dep, version) in dependencies) {
        buildFile.append("    ${dep.scope.label}(\"${dep.group}:${dep.name}:$version\")\n")
    }
    buildFile.append("}\n\n")

    settingsFile.append("pluginManagement {\n")
    settingsFile.append("    repositories {\n")
    for (pluginRepo in pluginRepos) {
        settingsFile.append("        $pluginRepo\n")
    }
    settingsFile.append("    }\n")
    settingsFile.append("}\n\n")

    buildFile.append("application.mainClass = \"kscript.EntryPointKt\"\n")

    for ((option, value) in options) when (option) {
        "wrapper.distributionBase",
        "wrapper.distributionPath",
        "wrapper.distributionUrl",
        "wrapper.networkTimeout",
        "wrapper.validateDistributionUrl",
        "wrapper.zipStoreBase",
        "wrapper.zipStorePath" -> if (
            rawFiles["gradle/wrapper/gradle-wrapper.properties"]
                ?.let(::String)
                ?.contains(option.removePrefix("wrapper.") + "=") != true
        ) {
            gradleWrapperProperties.append(option.removePrefix("wrapper.") + "=" + value + "\n")
        }

        "gradle.version" -> if ("wrapper.distributionUrl" in propertiesFile ||
            rawFiles["gradle/wrapper/gradle-wrapper.properties"]
                ?.let(::String)
                ?.contains("distributionUrl=") == true
        ) {
            error { "Can't enforce gradle version when `distributionUrl` is already set" }
        } else {
            gradleWrapperProperties.append("distributionUrl=https\\://services.gradle.org/distributions/gradle-$value-bin.zip\n")
        }

        "java.toolchain" -> buildFile.append("kotlin.javaToolchain($value)\n")
        "jvm.target" -> buildFile.append("kotlin.compilerOptions.jvmTarget = JvmTarget.fromTarget(\"$value\")\n")
        "jvm.arguments" -> buildFile.append("application.applicationDefaultJvmArgs.jvmArgs = ${toCode(readBashArgs(value))})\n")
        else -> warn { "Unrecognized option $option" }
        // TODO
    }

    for ((opt, def) in listOf(
        "distributionBase" to "GRADLE_USER_HOME",
        "distributionPath" to "wrapper/dists",
        "distributionUrl" to "https\\://services.gradle.org/distributions/gradle-9.0-bin.zip",
        "networkTimeout" to "10000",
        "validateDistributionUrl" to "true",
        "zipStoreBase" to "GRADLE_USER_HOME",
        "zipStorePath" to "wrapper/dists",
    )) {
        if (!gradleWrapperProperties.contains("$opt=")) {
            gradleWrapperProperties.append("$opt=$def\n")
        }
    }

    rawFiles["build.gradle.kts"] = buildFile.toString().toByteArray()
    rawFiles["settings.gradle.kts"] = settingsFile.toString().toByteArray()
    rawFiles["gradle.properties"] = propertiesFile.toString().toByteArray()
    rawFiles["gradle/wrapper/gradle-wrapper.properties"] = gradleWrapperProperties.toString().toByteArray()
    for ((path, appendices) in rawInserts) {
        rawFiles[path] = (rawFiles[path] ?: byteArrayOf()) + appendices.joinToString("\n").toByteArray()
    }

    return rawFiles
}

fun toCode(list: List<String>): String {
    fun String.escapeForKotlinString(): String {
        return buildString {
            append('"')
            this@escapeForKotlinString.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    '\$' -> append("\\$")
                    '\b' -> append("\\b")
                    '\'' -> append("\\'")
                    else -> {
                        if (char.code < 32 || char.code == 127) {
                            // Escape control characters
                            append("\\u${char.code.toString(16).padStart(4, '0')}")
                        } else {
                            append(char)
                        }
                    }
                }
            }
            append('"')
        }
    }

    return "listOf(${list.joinToString { it.escapeForKotlinString() }})"
}

fun readBashArgs(input: String): List<String> {
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var inSingleQuote = false
    var inDoubleQuote = false
    var escapeNext = false
    var i = 0

    while (i < input.length) {
        val c = input[i]

        when {
            escapeNext -> {
                // Handle escaped character
                current.append(c)
                escapeNext = false
                i++
            }

            c == '\\' -> {
                // Start escape sequence
                if (inSingleQuote) {
                    current.append("\\")
                    i++
                } else {
                    escapeNext = true
                    i++
                }
            }

            c == '\'' && !inDoubleQuote -> {
                // Toggle single quote
                inSingleQuote = !inSingleQuote
                i++
            }

            c == '"' && !inSingleQuote -> {
                // Toggle double quote
                inDoubleQuote = !inDoubleQuote
                i++
            }

            c.isWhitespace() && !inSingleQuote && !inDoubleQuote -> {
                // Space outside quotes - end of argument
                if (current.isNotEmpty()) {
                    result.add(current.toString())
                    current.clear()
                }
                i++
                // Skip any additional spaces
                while (i < input.length && input[i].isWhitespace()) i++
            }

            else -> {
                // Regular character
                current.append(c)
                i++
            }
        }
    }

    // Add the last argument if exists
    if (current.isNotEmpty()) {
        result.add(current.toString())
    }

    return result
}

context(ctx: ApplicationContext) fun resolvePath(path: String): String {
    val parts = path.split("/").flatMap { it.split(File.separator) }
    val result = mutableListOf<String>()
    for (part in parts) when (part) {
        ".." -> result.removeLastOrNull() ?: fatal { "Can't access outside of the project" }
        ".", "" -> Unit
        else -> result.add(part)
    }
    return result.joinToString("/")
}