//usr/bin/env java -jar /home/ldemis/kscript/kscript.jar "$0" -- "$@" ; exit

fun main(vararg args: String) {
    println("Hello, " + args.getOrElse(0) { "World" })
}