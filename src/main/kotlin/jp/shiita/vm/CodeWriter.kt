package jp.shiita.vm

import java.io.BufferedWriter
import java.io.Closeable
import java.io.FileWriter

class CodeWriter(path: String) : Closeable {
    private val writer = BufferedWriter(FileWriter(path))
    private var eqNum = 0
    private var gtNum = 0
    private var ltNum = 0

    override fun close() = writer.close()

    init {
        initStack()
        initEQ()
        initGT()
        initLT()
    }

    fun writeArithmetic(command: String) = when (command) {
        "add", "sub", "and", "or" -> {
            writeLines(listOf(
                "@SP",
                "AM=M-1",
                "D=M",
                "A=A-1",
                "M=M${opMap[command]}D"))
        }
        "eq", "gt", "lt" -> {
            val (num, jumpAddr) = when (command) {
                "eq" -> eqNum++ to ADDR_EQ
                "gt" -> gtNum++ to ADDR_GT
                "lt" -> ltNum++ to ADDR_LT
                else -> error("")
            }
            val commandU = command.toUpperCase()

            writeLines(listOf(
                "@RET_ADDRESS_$commandU$num",
                "D=A",
                "@$jumpAddr",
                "0;JMP",
                "(RET_ADDRESS_$commandU$num)"))
        }
        "neg", "not" -> {
            writeLines(listOf(
                "@SP",
                "A=M-1",
                "M=${opMap[command]}M"))
        }
        else  -> error("invalid command : \"$command\"")
    }

    fun writePushPop(commandType: Parser.CommandType, segment: String, index: Int) = when (commandType) {
        Parser.CommandType.PUSH -> {
            writeLines(listOf(
                "@$index",
                "D=A",
                "@SP",
                "AM=M+1",
                "A=A-1",
                "M=D"))
        }
        else  -> error("invalid command : \"$commandType $segment $index\"")
    }

    private fun initStack() = writeLines(listOf(
        "@256",
        "D=A",
        "@SP",
        "M=D",
        "@$ADDR_START",
        "0;JMP"))

    private fun initEQ() = writeLines(listOf(
        "@R15",
        "M=D",
        "@SP",
        "AM=M-1",
        "D=M",
        "A=A-1",
        "D=M-D",
        "M=0",
        "@END_EQ",
        "D;JNE",
        "@SP",
        "A=M-1",
        "M=-1",
        "(END_EQ)",
        "@R15",
        "A=M",
        "0;JMP"))

    private fun initGT() = writeLines(listOf(
        "@R15",
        "M=D",
        "@SP",
        "AM=M-1",
        "D=M",
        "A=A-1",
        "D=M-D",
        "M=0",
        "@END_GT",
        "D;JLE",
        "@SP",
        "A=M-1",
        "M=-1",
        "(END_GT)",
        "@R15",
        "A=M",
        "0;JMP"))

    private fun initLT() = writeLines(listOf(
        "@R15",
        "M=D",
        "@SP",
        "AM=M-1",
        "D=M",
        "A=A-1",
        "D=M-D",
        "M=0",
        "@END_LT",
        "D;JGE",
        "@SP",
        "A=M-1",
        "M=-1",
        "(END_LT)",
        "@R15",
        "A=M",
        "0;JMP"))

    private fun writeLines(lines: List<String>) = lines.forEach(::writeln)

    private fun writeln(line: String) {
        writer.write(line)
        writer.newLine()
    }

    companion object {
        private const val ADDR_EQ = 6
        private const val ADDR_GT = ADDR_EQ + 16
        private const val ADDR_LT = ADDR_GT + 16
        private const val ADDR_START = ADDR_LT + 16

        private val opMap = mapOf(
            "add" to "+",
            "sub" to "-",
            "and" to "&",
            "or"  to "|",
            "neg" to "-",
            "not" to "!")
    }
}