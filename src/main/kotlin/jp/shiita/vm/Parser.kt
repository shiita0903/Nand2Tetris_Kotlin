package jp.shiita.vm

import java.io.Closeable
import java.io.FileReader
import java.io.StreamTokenizer

class Parser(path: String) : Closeable {
    lateinit var commandType: CommandType
    val hasMoreCommands: Boolean
        get() = tokenizer.ttype != StreamTokenizer.TT_EOF
    var arg1: String? = null
    var arg2: Int? = null

    private var reader = FileReader(path)
    private val tokenizer = StreamTokenizer(reader)

    init {
        tokenizer.nextToken()
    }

    override fun close() {
        reader.close()
    }

    fun advance() {
        if (!hasMoreCommands) return

        commandType = when (tokenizer.sval) {
            in arithmeticCommands -> CommandType.ARITHMETIC
            "push" -> CommandType.PUSH
            "pop" -> CommandType.POP
            else -> TODO()
        }

        when (commandType) {
            CommandType.ARITHMETIC -> {
                arg1 = tokenizer.sval
                arg2 = null
            }
            CommandType.PUSH, CommandType.POP -> {
                tokenizer.nextToken()
                arg1 = tokenizer.sval
                when (arg1) {
                    "constant" -> {
                        tokenizer.nextToken()
                        arg2 = tokenizer.nval.toInt()
                    }
                    else -> TODO()
                }
            }
            else -> TODO()
        }
        tokenizer.nextToken()
    }

    enum class CommandType { ARITHMETIC, PUSH, POP, LABEL, GOTO, IF, FUNCTION, RETURN, CALL }

    companion object {
        private val arithmeticCommands = listOf("add", "sub", "neg", "eq", "gt", "lt", "and", "or", "not")
    }
}