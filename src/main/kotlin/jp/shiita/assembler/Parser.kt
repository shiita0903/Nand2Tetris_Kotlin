package jp.shiita.assembler

import java.io.Closeable
import java.io.InputStreamReader
import java.io.StreamTokenizer

class Parser(private val isr: InputStreamReader) : Closeable {
    lateinit var commandType: CommandType
    val hasMoreCommands: Boolean
        get() = tokenizer.ttype != StreamTokenizer.TT_EOF
    var symbol: String? = null
    var dest: String? = null
    var comp: String? = null
    var jump: String? = null

    private val tokenizer = StreamTokenizer(isr).apply {
        resetSyntax()
        wordChars('0'.toInt(), '9'.toInt())
        wordChars('a'.toInt(), 'z'.toInt())
        wordChars('A'.toInt(), 'Z'.toInt())
        listOf('@', '=', '+', '-', '!', '&', '|', ';', '(', ')', '_', '.', '$', ':')
            .map(Char::toInt)
            .forEach { c -> wordChars(c, c) }
        slashSlashComments(true)
    }

    init {
        setNextToken()
    }

    override fun close() = isr.close()

    fun advance() {
        if (!hasMoreCommands) return

        val token = tokenizer.sval
        commandType = if (token[0] == '@') CommandType.A else CommandType.C

        when (commandType) {
            CommandType.A -> symbol = token.drop(1)
            CommandType.C -> {
                dest = null
                jump = null

                var t = token
                if ('=' in t)  {
                    t.split('=').let {
                        dest = it[0]
                        t = it[1]
                    }
                }
                if (';' in t) {
                    t.split(';').let {
                        t = it[0]
                        jump = it[1]
                    }
                }
                comp = t
            }
            CommandType.L -> {}
        }
        setNextToken()
    }

    private fun setNextToken() {
        while (true) {
            when (tokenizer.nextToken()) {
                StreamTokenizer.TT_WORD, StreamTokenizer.TT_EOF -> return
            }
        }
    }

    enum class CommandType { A, C, L }
}