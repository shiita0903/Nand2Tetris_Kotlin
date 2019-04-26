package jp.shiita.assembler

import java.io.Closeable
import java.io.FileReader
import java.io.StreamTokenizer

class Parser(private val path: String, printSymbolTable: Boolean = false) : Closeable {
    lateinit var commandType: CommandType
        private set
    val hasMoreCommands: Boolean
        get() = tokenizer.ttype != StreamTokenizer.TT_EOF
    var symbol: Int = 0
        private set
    var dest: String? = null
        private set
    var comp: String? = null
        private set
    var jump: String? = null
        private set

    private var reader: FileReader? = null
    private lateinit var tokenizer: StreamTokenizer

    private var address = 0
    private var ramAddress = 16
    private var makeSymbolTableMode = true

    private val symbolTable = mutableMapOf(
            "SP" to 0,
            "LCL" to 1,
            "ARG" to 2,
            "THIS" to 3,
            "THAT" to 4,
            "SCREEN" to 16384,
            "KBD" to 24576
    ).apply {
        val registers = (0..15).map { "R$it" to it }
        putAll(registers)
    }

    init {
        makeSymbolTable()
        if (printSymbolTable) println("symbolTable = $symbolTable")
    }

    override fun close() {
        reader?.close()
    }

    fun advance() {
        if (!hasMoreCommands) return

        val token = tokenizer.sval
        commandType = when (token[0]) {
            '@' -> CommandType.A
            '(' -> CommandType.L
            else -> CommandType.C
        }

        when (commandType) {
            CommandType.A -> {
                address++
                if (!makeSymbolTableMode) {
                    val t = token.drop(1)
                    runCatching { t.toInt() }
                            .onSuccess { symbol = it }
                            .onFailure { symbol = symbolTable.getOrPut(t) { ramAddress++ } }
                }
            }
            CommandType.C -> {
                address++
                if (!makeSymbolTableMode) {
                    dest = null
                    jump = null

                    var t = token
                    if ('=' in t) {
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
            }
            CommandType.L -> {
                if (makeSymbolTableMode)
                    symbolTable[token.substring(1, token.length - 1)] = address
            }
        }
        setNextToken()
    }

    private fun getTokenizer(): StreamTokenizer {
        reader?.close()
        reader = FileReader(path)
        return StreamTokenizer(reader).apply {
            resetSyntax()
            wordChars('0'.toInt(), '9'.toInt())
            wordChars('a'.toInt(), 'z'.toInt())
            wordChars('A'.toInt(), 'Z'.toInt())
            listOf('@', '=', '+', '-', '!', '&', '|', ';', '(', ')', '_', '.', '$', ':')
                    .map(Char::toInt)
                    .forEach { c -> wordChars(c, c) }
            slashSlashComments(true)
        }
    }

    private fun makeSymbolTable() {
        tokenizer = getTokenizer()
        setNextToken()
        while (hasMoreCommands) advance()
        makeSymbolTableMode = false
        tokenizer = getTokenizer()
        setNextToken()
    }

    private fun setNextToken() {
        val targetToken = listOf(StreamTokenizer.TT_WORD, StreamTokenizer.TT_EOF)
        while (tokenizer.nextToken() !in targetToken);
    }

    enum class CommandType { A, C, L }
}