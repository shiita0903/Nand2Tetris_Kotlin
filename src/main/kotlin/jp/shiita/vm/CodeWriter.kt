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
    private var callNum = 0
    private var currentFunctionName = ""

    override fun close() = writer.close()

    fun writeInit() {
        initStack()
        initEQ()
        initGT()
        initLT()
        initCall()
        initReturn()
        writeCall("Sys.init", 0)
    }

    fun writeArithmetic(command: String) = when (command) {
        "add", "sub", "and", "or" -> {
            writeLines(
                listOf(
                    "@SP",
                    "AM=M-1",
                    "D=M",
                    "A=A-1",
                    "M=M${opMap[command]}D"
                )
            )
        }
        "eq", "gt", "lt" -> {
            val (num, jumpAddr) = when (command) {
                "eq" -> eqNum++ to ADDR_EQ
                "gt" -> gtNum++ to ADDR_GT
                "lt" -> ltNum++ to ADDR_LT
                else -> error("")
            }
            val commandU = command.toUpperCase()

            writeLines(
                listOf(
                    "@RET_ADDRESS_$commandU$num",
                    "D=A",
                    "@$jumpAddr",
                    "0;JMP",
                    "(RET_ADDRESS_$commandU$num)"
                )
            )
        }
        "neg", "not" -> {
            writeLines(
                listOf(
                    "@SP",
                    "A=M-1",
                    "M=${opMap[command]}M"
                )
            )
        }
        else -> error("invalid command : \"$command\"")
    }

    fun writePushPop(commandType: Parser.CommandType, segment: String, index: Int) = when (commandType) {
        Parser.CommandType.PUSH -> {
            when (segment) {
                "constant" -> writeLines(
                    listOf(
                        "@$index",
                        "D=A"
                    )
                )
                in symbolMap.keys -> writeLines(
                    listOf(
                        "@${symbolMap[segment]}",
                        "D=M",
                        "@$index",
                        "A=D+A",
                        "D=M"
                    )
                )
                in baseMap.keys -> writeLines(
                    listOf(
                        "@${baseMap.getOrDefault(segment, 0) + index}",
                        "D=M"
                    )
                )
                "static" -> writeLines(
                    listOf(
                        "@$vmFileName.$index",
                        "D=M"
                    )
                )
                else -> error("invalid segment : \"$commandType $segment $index\"")
            }
            writeLines(
                listOf(
                    "@SP",
                    "AM=M+1",
                    "A=A-1",
                    "M=D"
                )
            )
        }
        Parser.CommandType.POP -> {
            when (segment) {
                in symbolMap.keys -> writeLines(
                    listOf(
                        "@${symbolMap[segment]}",
                        "D=M",
                        "@$index",
                        "D=D+A"
                    )
                )
                in baseMap.keys -> writeLines(
                    listOf(
                        "@${baseMap.getOrDefault(segment, 0) + index}",
                        "D=A"
                    )
                )
                "static" -> writeLines(
                    listOf(
                        "@$vmFileName.$index",
                        "D=A"
                    )
                )
                else -> error("invalid segment : \"$commandType $segment $index\"")
            }
            writeLines(
                listOf(
                    "@R13",
                    "M=D",
                    "@SP",
                    "AM=M-1",
                    "D=M",
                    "@R13",
                    "A=M",
                    "M=D"
                )
            )
        }
        else -> error("invalid command : \"$commandType $segment $index\"")
    }

    fun writeLabel(label: String) = writeln("($currentFunctionName\$$label)")

    fun writeGoto(label: String) = writeLines(
        listOf(
            "@$currentFunctionName\$$label",
            "0;JMP"
        )
    )

    fun writeIf(label: String) = writeLines(
        listOf(
            "@SP",
            "AM=M-1",
            "D=M",
            "@$currentFunctionName\$$label",
            "D;JNE"
        )
    )

    fun writeCall(functionName: String, numArgs: Int) = writeLines(
        listOf(
            "@$numArgs",
            "D=A",
            "@R13",
            "M=D",
            "@$functionName",
            "D=A",
            "@R14",
            "M=D",
            "@RET_ADDRESS_CALL$callNum",
            "D=A",
            "@$ADDR_CALL",
            "0;JMP",
            "(RET_ADDRESS_CALL${callNum++})"
        )
    )

    fun writeReturn() = writeLines(
        listOf(
            "@$ADDR_RETURN",
            "0;JMP"
        )
    )

    fun writeFunction(functionName: String, numLocals: Int) {
        currentFunctionName = functionName
        writeln("($functionName)")

        if (numLocals > 0) writeLines(
            listOf(
                "@$numLocals",
                "D=A",
                "(LOOP_$functionName)",
                "D=D-1",
                "@SP",
                "AM=M+1",
                "A=A-1",
                "M=0",
                "@LOOP_$functionName",
                "D;JGT"
            )
        )
    }

    private fun initStack() = writeLines(
        listOf(
            "@256",
            "D=A",
            "@SP",
            "M=D",
            "@$ADDR_START",
            "0;JMP"
        )
    )

    private fun initEQ() = writeLines(
        listOf(
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
            "0;JMP"
        )
    )

    private fun initGT() = writeLines(
        listOf(
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
            "0;JMP"
        )
    )

    private fun initLT() = writeLines(
        listOf(
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
            "0;JMP"
        )
    )

    private fun initCall() {
        // store return address
        writeLines(
            listOf(
                "@SP",
                "A=M",
                "M=D"
            )
        )

        // store segment base address
        listOf("@LCL", "@ARG", "@THIS", "@THAT").forEach { label ->
            writeLines(
                listOf(
                    label,
                    "D=M",
                    "@SP",
                    "AM=M+1",
                    "M=D"
                )
            )
        }

        // set ARG and LCL base address
        writeLines(
            listOf(
                "@4",
                "D=A",
                "@R13",
                "D=D+M",
                "@SP",
                "D=M-D",
                "@ARG",
                "M=D",
                "@SP",
                "MD=M+1",
                "@LCL",
                "M=D"
            )
        )

        // call function
        writeLines(
            listOf(
                "@R14",
                "A=M",
                "0;JMP"
            )
        )
    }

    private fun initReturn() {
        // set R13 as return address
        writeLines(
            listOf(
                "@5",
                "D=A",
                "@LCL",
                "A=M-D",
                "D=M",
                "@R13",
                "M=D"
            )
        )

        // set SP and return value
        writeLines(
            listOf(
                "@SP",
                "AM=M-1",
                "D=M",
                "@ARG",
                "A=M",
                "M=D",
                "D=A",
                "@SP",
                "M=D+1"
            )
        )

        // restore segment base address
        writeLines(
            listOf(
                "@LCL",
                "D=M",
                "@R14",
                "AM=D-1",
                "D=M"
            )
        )
        listOf("@THAT", "@THIS", "@ARG").forEach { label ->
            writeLines(
                listOf(
                    label,
                    "M=D",
                    "@R14",
                    "AM=M-1",
                    "D=M"
                )
            )
        }
        writeLines(
            listOf(
                "@LCL",
                "M=D"
            )
        )

        // return
        writeLines(
            listOf(
                "@R13",
                "A=M",
                "0;JMP"
            )
        )
    }

    private fun writeLines(lines: List<String>) = lines.forEach(::writeln)

    private fun writeln(line: String) {
        writer.write(line)
        writer.newLine()
    }

    companion object {
        private const val ADDR_EQ = 6
        private const val ADDR_GT = ADDR_EQ + 16
        private const val ADDR_LT = ADDR_GT + 16
        private const val ADDR_CALL = ADDR_LT + 16
        private const val ADDR_RETURN = ADDR_CALL + 38
        private const val ADDR_START = ADDR_RETURN + 41

        private val opMap = mapOf(
            "add" to "+",
            "sub" to "-",
            "and" to "&",
            "or" to "|",
            "neg" to "-",
            "not" to "!"
        )

        private val baseMap = mapOf(
            "pointer" to 3,
            "temp" to 5
        )

        private val symbolMap = mapOf(
            "local" to "LCL",
            "argument" to "ARG",
            "this" to "THIS",
            "that" to "THAT"
        )
    }
}