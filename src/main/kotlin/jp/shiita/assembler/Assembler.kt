package jp.shiita.assembler

import java.io.BufferedWriter
import java.io.FileWriter

class Assembler(private val asmPath: String, private val printInfo: Boolean = false) {
    private val hackPath = asmPath.replace(".asm", ".hack")

    fun assemble() {
        Parser(asmPath, printInfo).use { parser ->
            BufferedWriter(FileWriter(hackPath)).use { writer ->
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
                        Parser.CommandType.L -> {}
                    }
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

fun main(args: Array<String>) = Assembler("src/main/resource/${args[0]}", true).assemble()

