fun main(vararg args: String) {
    println("Hello, " + args.getOrElse(0) { "World" })
}