package jp.shiita.assembler

import java.io.*

fun main(args: Array<String>) {
    val asmPath = "src/main/resource/${args[0]}"
    val hackPath = "src/main/resource/${args[0].replace("asm", "hack")}"

    Parser(FileReader(asmPath)).use { parser ->
    BufferedWriter(FileWriter(hackPath)).use { writer ->
        loop@ while (parser.hasMoreCommands) {
            parser.advance()
            when (parser.commandType) {
                Parser.CommandType.A -> {
                    val const = parser.symbol?.toInt() ?: continue@loop
                    writeWord(writer, const)
                }
                Parser.CommandType.C -> {
                    var word = 0b111 shl 13
                    parser.comp?.let { word = word or (Code.comp(it) shl 6) }
                    parser.dest?.let { word = word or (Code.dest(it) shl 3) }
                    parser.jump?.let { word = word or Code.jump(it) }
                    writeWord(writer, word)
                }
            }
        }
    }}
}

fun writeWord(writer: BufferedWriter, word: Int) {
    val str = word.toString(radix = 2).padStart(16, '0')
    writer.write(str)
    writer.newLine()
    println(str)
}