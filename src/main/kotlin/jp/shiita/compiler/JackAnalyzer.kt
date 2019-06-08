package jp.shiita.compiler

import java.io.File

class JackAnalyzer {

    fun analyze(file: File) {
        if (file.isFile) {
            if (file.extension == "jack") {
                compile(file)
                return
            }
        } else {
            val jackFiles = file.listFiles { f -> f.extension == "jack" }
            if (!jackFiles.isNullOrEmpty()) {
                jackFiles.forEach(::compile)
                return
            }
        }

        println(".jack file is not found")
    }

    private fun compile(file: File) {
        val tokenizer = JackTokenizer(file.path)
        val vmPath = "${file.path.substringBeforeLast(".")}.vm"
        val writer = VMWriter(vmPath)

        CompilationEngine(tokenizer, writer).use { it.compile() }
        println("compile is finished : [${file.path}]")
    }
}

fun main(args: Array<String>) {
    val file = File("src/main/resources/compiler/${args[0]}")
    JackAnalyzer().analyze(file)
}