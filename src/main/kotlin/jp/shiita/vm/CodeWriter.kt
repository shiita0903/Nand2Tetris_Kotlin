package jp.shiita.vm

import java.io.BufferedWriter
import java.io.Closeable
import java.io.FileWriter

class CodeWriter(path: String) : Closeable {
    var vmFileName: String = ""
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
        else -> error("invalid command : \"$command\"")
    }

    fun writePushPop(commandType: Parser.CommandType, segment: String, index: Int) = when (commandType) {
        Parser.CommandType.PUSH -> {
            when (segment) {
                "constant" -> writeLines(listOf(
                        "@$index",
                        "D=A"))
                in symbolMap.keys -> writeLines(listOf(
                        "@${symbolMap[segment]}",
                        "D=M",
                        "@$index",
                        "A=D+A",
                        "D=M"))
                in baseMap.keys -> writeLines(listOf(
                        "@${baseMap.getOrDefault(segment, 0) + index}",
                        "D=M"))
                "static" -> writeLines(listOf(
                        "@$vmFileName.$index",
                        "D=M"
                ))
                else -> error("invalid segment : \"$commandType $segment $index\"")
            }
            writeLines(listOf(
                    "@SP",
                    "AM=M+1",
                    "A=A-1",
                    "M=D"))
        }
        Parser.CommandType.POP -> {
            when (segment) {
                in symbolMap.keys -> writeLines(listOf(
                        "@${symbolMap[segment]}",
                        "D=M",
                        "@$index",
                        "D=D+A"))
                in baseMap.keys -> writeLines(listOf(
                        "@${baseMap.getOrDefault(segment, 0) + index}",
                        "D=A"))
                "static" -> writeLines(listOf(
                        "@$vmFileName.$index",
                        "D=A"))
                else -> error("invalid segment : \"$commandType $segment $index\"")
            }
            writeLines(listOf(
                    "@R13",
                    "M=D",
                    "@SP",
                    "AM=M-1",
                    "D=M",
                    "@R13",
                    "A=M",
                    "M=D"))
        }
        else -> error("invalid command : \"$commandType $segment $index\"")
    }

    private fun initStack() = writeLines(listOf(
            "@256",
            "D=A",
            "@SP",
            "M=D",
            "@$ADDR_START",
            "0;JMP"))

    private fun initEQ() = writeLines(listOf(
            "@R13",
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
            "@R13",
            "A=M",
            "0;JMP"))

    private fun initGT() = writeLines(listOf(
            "@R13",
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
            "@R13",
            "A=M",
            "0;JMP"))

    private fun initLT() = writeLines(listOf(
            "@R13",
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
            "@R13",
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
                "or" to "|",
                "neg" to "-",
                "not" to "!")

        private val baseMap = mapOf(
                "pointer" to 3,
                "temp" to 5)

        private val symbolMap = mapOf(
                "local" to "LCL",
                "argument" to "ARG",
                "this" to "THIS",
                "that" to "THAT")
    }
}