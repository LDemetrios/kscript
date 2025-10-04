package org.ldemetrios.kscript

import kotlin.system.exitProcess

enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
    FATAL,
}

context(ctx: ApplicationContext) inline fun log(level: LogLevel, message: () -> String) {
    if (level >= ctx.logLevel) {
        System.err.println("[$level] ${message()}")
    }
}
context(ctx: ApplicationContext) inline fun debug(message: () -> String) {
    log(LogLevel.DEBUG, message)
}

context(ctx: ApplicationContext) inline fun info(message: () -> String) {
    log(LogLevel.INFO, message)
}

context(ctx: ApplicationContext) inline fun warn(message: () -> String) {
    log(LogLevel.WARN, message)
}

context(ctx: ApplicationContext) inline fun logError(message: () -> String) {
    log(LogLevel.ERROR, message)
}

context(ctx: ApplicationContext) inline fun fatal(message: () -> String) : Nothing {
    log(LogLevel.FATAL, message)
    Throwable().printStackTrace()
    exitProcess(1)
}
