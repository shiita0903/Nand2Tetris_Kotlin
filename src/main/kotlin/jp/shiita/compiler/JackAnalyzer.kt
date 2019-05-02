package jp.shiita.compiler

import java.io.File

class JackAnalyzer {

    fun analyze(file: File) {
        if (file.isFile) {
            if (file.extension == "jack") {
                parse(file)
                return
            }
        } else {
            val jackFiles = file.listFiles { f -> f.extension == "jack" }
            if (!jackFiles.isNullOrEmpty()) {
                jackFiles.forEach(::parse)
                return
            }
        }

        println(".jack file is not found")
    }

    private fun parse(file: File) {
        val tokenizer = JackTokenizer(file.path)
        val xmlPath = "${file.path.substringBeforeLast(".")}.xml"

        CompilationEngine(tokenizer, xmlPath).use { it.compile() }
        println("parse is finished : [${file.path}]")
    }
}

fun main(args: Array<String>) {
    val file = File("src/main/resources/compiler/${args[0]}")
    JackAnalyzer().analyze(file)
}