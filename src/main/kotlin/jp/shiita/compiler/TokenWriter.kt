package jp.shiita.compiler

import java.io.BufferedWriter
import java.io.Closeable
import java.io.File
import java.io.FileWriter

class TokenWriter(private val tokenizer: JackTokenizer, path: String) : Closeable {
    private val writer = BufferedWriter(FileWriter(path))

    override fun close() {
        tokenizer.close()
        writer.close()
    }

    fun tokenize() {
        writeln("<tokens>")

        while (tokenizer.hasMoreTokens) {
            tokenizer.advance()
            when (tokenizer.tokenType) {
                JackTokenizer.TokenType.KEYWORD -> writeln("<keyword> ${tokenizer.keyword!!.name.toLowerCase()} </keyword>")
                JackTokenizer.TokenType.SYMBOL -> {
                    val symbol = escapeMap.getOrElse(tokenizer.symbol!!) { tokenizer.symbol.toString() }
                    writeln("<symbol> $symbol </symbol>")
                }
                JackTokenizer.TokenType.IDENTIFIER -> writeln("<identifier> ${tokenizer.identifier} </identifier>")
                JackTokenizer.TokenType.INT_CONST -> writeln("<integerConstant> ${tokenizer.intVal} </integerConstant>")
                JackTokenizer.TokenType.STRING_CONST -> writeln("<stringConstant> ${tokenizer.stringVal} </stringConstant>")
            }
        }

        writeln("</tokens>")
    }

    private fun writeln(line: String) {
        writer.write(line)
        writer.newLine()
    }

    companion object {
        private val escapeMap = mapOf(
                '<' to "&lt;",
                '>' to "&gt;",
                '&' to "&amp;")
    }
}

fun main(args: Array<String>) {
    val file = File("src/main/resource/compiler/${args[0]}")

    fun tokenize(f: File) {
        val tokenizer = JackTokenizer(f.path)
        val xmlPath = "${f.path.substringBeforeLast(".")}T.xml"

        TokenWriter(tokenizer, xmlPath).use { it.tokenize() }
        println("tokenize is finished : [${f.path}]")
    }

    if (file.isFile) {
        if (file.extension == "jack") {
            tokenize(file)
            return
        }
    } else {
        val jackFiles = file.listFiles { f -> f.extension == "jack" }
        if (!jackFiles.isNullOrEmpty()) {
            jackFiles.forEach(::tokenize)
            return
        }
    }

    println(".jack file is not found")
}