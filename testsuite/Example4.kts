//usr/bin/env java -jar /home/ldemis/kscript/kscript.jar --debug --config-file "$(dirname "$0")"/Example4.config "$0" -- "$@" ; exit

//! #experimental(context-parameters)

interface Group<T> {
    fun add(a: T, b: T): T
    fun subtract(a: T, b: T): T
    fun zero(): T
}

object MultiplicativeGroup : Group<Double> {
    override fun add(a: Double, b: Double) = a * b
    override fun subtract(a: Double, b: Double) = a / b
    override fun zero(): Double = 1.0
}

context(group: Group<T>) operator fun <T> T.plus(other: T) = group.add(this, other)
context(group: Group<T>) operator fun <T> T.minus(other: T) = group.subtract(this, other)
context(group: Group<T>) operator fun <T> T.unaryMinus() = group.subtract(group.zero(), this)

context(group: Group<T>) fun <T> List<T>.sum() = fold(group.zero()) { a, b -> a + b }

fun main() {
    println(
        with(MultiplicativeGroup) {
            listOf(1.0, 2.0, 3.0, 5.0).sum() // 30.0
        }
    )
}



