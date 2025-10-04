# KScript

This is a cli app to launch Kotlin scripts. It allows adding dependencies and other configuration through configuration
in the comments.

*__Disclaimer__: This tool was created in a cave, with a bunch of scraps. I'm not planning to continue development apart
from fixing minor bugs. However, I welcome good pull requests. There are a few ideas that I'm too lazy to implement, but
you may take a look at them (see ["Contribution"](https://github.com/LDemetrios/kscript#Contribution)).*

## Installation (kinda)

There's a kscript.jar in the repository, you can start from it:

```bash
 java -jar kscript.jar
```

If you want to build it from source files:

```bash
gradle shadowJar
```

(Or use appropriate gradle wrapper for your OS), and then find result in ./build/libs/kscript-all.jar

## Usage

```bash
java -jar kscript.jar --help
```

Duh.

It accepts source file as a "free argument"

```bash
java -jar kscript.jar Script.kts 
```

Or it can read from stdin:

```bash
echo 'fun main() = println("Hello, world")' | java -jar kscript.jar --stdin
```

You can also use `--config-file` (`-c`) option to include configuration from another file:

```bash
java -jar kscript.jar --config-file common.config Script.kts
```

See [`testsuite`](./testsuite) for concrete examples.

All arguments after `--` are treated as the script arguments.

Shebang will work, but if you work in IDEa and it doesn't think shebang is a valid part of a script, you can use an
empty path part trick:

```kts
//usr/bin/env java -jar /path/to/kscript.jar "$0" -- "$@" ; exit

fun main(vararg args: String) {
    println("Hello, " + args.getOrElse(0) { "World" })
}
```

When you make a file executable, and it doesn't have shebang, your shell is the default runner. It sees `//usr/bin/env`
and interprets it as an executable with parameters `java ...`. In turn, `env` finds `java` in path and calls it with
arguments `-jar kscript.jar ...`, and executes jar with arguments `"$0" -- "$@"`, which are initial file (`"$0"`) and
the rest of the arguments (`"$@"`) after `--`. Then, calling it with arguments will work pretty much as you'd expect:

```bash
./testsuite/Example.kts LDemetrios
```

prints `Hello, LDemetrios`.

## Config syntax

First of all, there are dependencies, plugins, and repositories where to search for those dependencies and plugins.

```kts
//!repository(gradlePluginPortal)
//!plugin("io.freefair.aspectj.post-compile-weaving", "8.13.1")
//!implementation("org.aspectj:aspectjrt:1.9.23")
//!implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0")
```

For repositories, you can use both just built-in ones (`gradlePluginPortal` here), and custom:

```kts
//!repository(maven, "https://jitpack.io/")
```

Then, there are the functions. I hate pure off-side-rule and was too lazy to implement a fine syntax, so here's
middle-ground:

```kts
//! define experimental(feature)
//! | rawInsert(build.gradle.kts)
//! | | kotlin {
//! | |     compilerOptions {
//! | |         freeCompilerArgs.add("-X#feature")
//! | |     }
//! | | }
//
// #experimental(inline-classes)
// #experimental(context-parameters)
```

You can't have comma as part of an argument. You can't have parameters names, one of which is a prefix of another one.
`rawInsert` is another thing you have: well, it's what written on the box.

And, there are properties: `gradle.version`, `java.toolchain`, `jvm.target`, `jvm.arguments`, which are quite self-explanatory. I will probably add some more later.

## Contribution

You can use this tool under "do whatever the f you want" terms, also known as Copyleft.

If you experience bugs, you can open a GitHub issue, and I will probably address it some day. I am actively using this tool, so yeah, bugs won't live forever. If you have a proposal for
improvement, however... Well, sorry, I have a bunch of projects I'm more interested in (check out my GitHub, I guess?), so you
should probably implement it yourself, and make a pull request. For example, it would be great to have a more powerful function system, for example, a C-preprocessor integrated, or a Clojure-like macro system. Also, it would be great to have a dedicated for user-wide and system-wide configs, location of which is determined based on operating system. Also, it would be great to have some sort of "stdlib" of common configuration options. Or, at the very least, it would be neat to have more robust config parser and senseful error messages.    



