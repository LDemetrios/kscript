//usr/bin/env java -jar /home/ldemis/kscript/kscript.jar "$0" -- "$@" ; exit

//!repository(gradlePluginPortal)
//!plugin("io.freefair.aspectj.post-compile-weaving", "8.13.1")
//!implementation("org.aspectj:aspectjrt:1.9.23")
//!implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0")

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import kotlin.system.exitProcess

@Retention(AnnotationRetention.RUNTIME)
annotation class NoExcept

@Aspect
public class NoExceptAspect {
    @Around("@annotation(kscript.NoExcept)")
    fun wrapWithTryCatch(joinPoint: ProceedingJoinPoint): Any? = try {
        joinPoint.proceed();
    } catch (t: Throwable) {
        System.err.println("Exception in @NoExcept method: " + joinPoint.getSignature());
        t.printStackTrace();
        exitProcess(1);
    }
}

fun main() {
    Thread {
        while (true) {
            // This prevents JVM from shutting down if main fails.
        }
    }.start()
    val points: List<Point?> = listOf<Point?>(
        Point(1, 2),
        Point(3, 4)
    )
    println(points.stream().sorted().toList())
}

data class Point(val x: Int, val y: Int) : Comparable<Point?> {
    @NoExcept
    override fun compareTo(point: Point?): Int = throw RuntimeException("Failure")
}
