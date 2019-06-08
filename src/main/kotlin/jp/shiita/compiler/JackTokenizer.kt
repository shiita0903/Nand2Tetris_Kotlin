package jp.shiita.compiler

import java.io.Closeable
import java.io.FileReader
import java.io.StreamTokenizer

class JackTokenizer(path: String) : Closeable {
    lateinit var tokenType: TokenType
        private set
    val hasMoreTokens: Boolean
        get() = tokenizer.ttype != StreamTokenizer.TT_EOF
    var keyword: Keyword? = null
        private set
    var symbol: Char? = null
        private set
    var identifier: String? = null
        private set
    var intVal: Int? = null
        private set
    var stringVal: String? = null
        private set

    private var reader = FileReader(path)
    private val tokenizer = StreamTokenizer(reader).apply {
        listOf('_')
            .map(Char::toInt)
            .forEach { c -> wordChars(c, c) }

        symbols.forEach(::ordinaryChar)

        slashSlashComments(true)
        slashStarComments(true)
    }

    init {
        tokenizer.nextToken()
    }

    override fun close() = reader.close()

    fun advance() {
        if (!hasMoreTokens) return

        tokenType = when (tokenizer.ttype) {
            StreamTokenizer.TT_WORD -> {
                when (tokenizer.sval) {
                    in keywords -> TokenType.KEYWORD
                    else -> TokenType.IDENTIFIER
                }
            }
            StreamTokenizer.TT_NUMBER -> TokenType.INT_CONST
            '"'.toInt() -> TokenType.STRING_CONST
            in symbols -> TokenType.SYMBOL
            else -> error("invalid token : \"${tokenizer.ttype.toChar()}\"")
        }

        keyword = null
        symbol = null
        identifier = null
        intVal = null
        stringVal = null
        when (tokenType) {
            TokenType.KEYWORD -> keyword = Keyword.values()[keywords.indexOf(tokenizer.sval)]
            TokenType.SYMBOL -> symbol = tokenizer.ttype.toChar()
            TokenType.IDENTIFIER -> identifier = tokenizer.sval
            TokenType.INT_CONST -> intVal = tokenizer.nval.toInt()
            TokenType.STRING_CONST -> stringVal = tokenizer.sval
        }

        tokenizer.nextToken()
    }

    enum class TokenType { KEYWORD, SYMBOL, IDENTIFIER, INT_CONST, STRING_CONST }

    enum class Keyword {
        CLASS, METHOD, FUNCTION, CONSTRUCTOR, INT, BOOLEAN, CHAR, VOID, VAR,
        STATIC, FIELD, LET, DO, IF, ELSE, WHILE, RETURN, TRUE, FALSE, NULL, THIS;

        fun getLower() = name.toLowerCase()
    }

    companion object {
        private val keywords = Keyword.values().map(Keyword::getLower)
        private val symbols: List<Int> = listOf(
            '{', '}', '(', ')', '[', ']', '.', ',', ';',
            '+', '-', '*', '/', '&', '|', '<', '>', '=', '~'
        ).map(Char::toInt)
    }
}