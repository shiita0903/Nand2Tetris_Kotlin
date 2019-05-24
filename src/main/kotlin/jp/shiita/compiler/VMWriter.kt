package jp.shiita.compiler

import java.io.BufferedWriter
import java.io.Closeable
import java.io.FileWriter

class VMWriter(path: String) : Closeable {
    private val writer = BufferedWriter(FileWriter(path))

    override fun close() = writer.close()

    fun writePush(segment: Segment, index: Int) = writeln("push ${segment.name.toLowerCase()} $index")

    fun writePop(segment: Segment, index: Int) = writeln("pop ${segment.name.toLowerCase()} $index")

    fun writeArithmetic(command: Command) = writeln(command.name.toLowerCase())

    fun writeLable(label: String) = writeln("label $label")

    fun writeGoto(label: String) = writeln("goto $label")

    fun writeIf(label: String) = writeln("if-goto $label")

    fun writeCall(name: String, nArgs: Int) = writeln("call $name $nArgs")

    fun writeFunction(name: String, nLocals: Int) = writeln("function $name $nLocals")

    fun writeReturn() = writeln("return")

    private fun writeln(line: String) {
        writer.write(line)
        writer.newLine()
    }

    enum class Segment { CONST, ARG, LOCAL, STATIC, THIS, THAT, POINTER, TEMP }

    enum class Command { ADD, SUB, NEG, EQ, GT, LT, AND, OR, NOT }
}