package jp.shiita.assembler

import jp.shiita.extensions.replaced
import java.io.BufferedWriter
import java.io.Closeable
import java.io.File
import java.io.FileWriter

class Assembler(
        private val parser: Parser,
        private val writer: BufferedWriter,
        private val printInfo: Boolean
) : Closeable {

    override fun close() {
        parser.close()
        writer.close()
    }

    fun assemble() {
        while (parser.hasMoreCommands) {
            parser.advance()
            when (parser.commandType) {
                Parser.CommandType.A -> {
                    val const = parser.symbol
                    writeWord(writer, const)
                }
                Parser.CommandType.C -> {
                    var word = 0b111 shl 13
                    parser.comp?.let { word = word or (Code.comp(it) shl 6) }
                    parser.dest?.let { word = word or (Code.dest(it) shl 3) }
                    parser.jump?.let { word = word or Code.jump(it) }
                    writeWord(writer, word)
                }
                Parser.CommandType.L -> {
                }
            }
        }
    }

    private fun writeWord(writer: BufferedWriter, word: Int) {
        val str = word.toString(radix = 2).padStart(16, '0')
        writer.write(str)
        writer.newLine()
        if (printInfo) println(str)
    }
}

fun main(args: Array<String>) {
    val file = File("src/main/resource/assembler/${args[0]}")

    if (file.extension == "asm") {
        val parser = Parser(file.path, printSymbolTable = true)
        val writer = BufferedWriter(FileWriter(file.replaced(extension = "hack").path))

        Assembler(parser, writer, printInfo = true).use { it.assemble() }
        println("assemble is finished")
    } else {
        println(".asm file is not found")
    }
}

