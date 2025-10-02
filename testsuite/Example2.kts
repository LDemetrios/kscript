//usr/bin/env java -jar /home/ldemis/kscript/kscript.jar "$0" -- "$@" ; exit

//!implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0")

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

data class Person(val name: String, val age: Int)

fun main(args: Array<String>) {
    println("${args.size} arguments")
    val json = """{"name": "Alice", "age": 30}"""
    val mapper = jacksonObjectMapper()

    val person: Person = mapper.readValue(json)
    println("Hello, ${person.name}! You are ${person.age} years old.")
}