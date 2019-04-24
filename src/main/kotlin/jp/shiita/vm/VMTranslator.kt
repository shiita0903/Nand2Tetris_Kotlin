package jp.shiita.vm

import java.io.Closeable
import java.io.File

class VMTranslator(private val parsers: List<Parser>, private val writer: CodeWriter) : Closeable {

    override fun close() {
        parsers.forEach { it.close() }
        writer.close()
    }

    fun translate() {
        parsers.forEach { parser ->
            while (parser.hasMoreCommands) {
                parser.advance()
                when (parser.commandType) {
                    Parser.CommandType.ARITHMETIC -> writer.writeArithmetic(parser.arg1!!)
                    Parser.CommandType.PUSH -> writer.writePushPop(parser.commandType, parser.arg1!!, parser.arg2!!)
                    Parser.CommandType.POP -> {}
                    Parser.CommandType.LABEL -> {}
                    Parser.CommandType.GOTO -> {}
                    Parser.CommandType.IF -> {}
                    Parser.CommandType.FUNCTION -> {}
                    Parser.CommandType.RETURN -> {}
                    Parser.CommandType.CALL -> {}
                }
            }
        }
    }
}

fun main(args: Array<String>) {
    val file = File("src/main/resource/vm/${args[0]}")

    if (file.isFile) {
        if (file.extension == "vm") {
            val parsers = listOf(Parser(file.path))
            val writer = CodeWriter(file.path.replace(".vm", ".asm"))

            VMTranslator(parsers, writer).use { it.translate() }
            println("translation is finished")
            return
        }
    }
    else {
        val vmFiles = file.listFiles { f -> f.extension == "vm" }
        if (!vmFiles.isNullOrEmpty()) {
            val parsers = vmFiles.map { Parser(it.path) }
            val writer = CodeWriter("$file${File.separator}${file.name}.asm")

            VMTranslator(parsers, writer).use { it.translate() }
            println("translation is finished")
            return
        }
    }

    println(".vm file is not found")
}