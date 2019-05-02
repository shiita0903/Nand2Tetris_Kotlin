package jp.shiita.compiler

import jp.shiita.compiler.JackTokenizer.Keyword
import jp.shiita.compiler.JackTokenizer.TokenType
import java.io.BufferedWriter
import java.io.Closeable
import java.io.FileWriter

class CompilationEngine(
    private val tokenizer: JackTokenizer,
    path: String
) : Closeable {
    private val writer = BufferedWriter(FileWriter(path))
    private var indentNum = 0
    private val isIdentifier
        get() = tokenizer.tokenType == TokenType.IDENTIFIER
    private val isIntConst
        get() = tokenizer.tokenType == TokenType.INT_CONST
    private val isStringConst
        get() = tokenizer.tokenType == TokenType.STRING_CONST
    private val isClassVarDec
        get() = tokenizer.keyword in classVarDecKeywords
    private val isSubroutineDec
        get() = tokenizer.keyword in subroutineDecKeywords
    private val isBuiltInType
        get() = tokenizer.keyword in buildInTypeKeywords
    private val isKeywordConstant
        get() = tokenizer.keyword in constantKeywords
    private val isUnaryOpSymbols
        get() = tokenizer.symbol in unaryOpSymbols
    private val isVoid
        get() = tokenizer.keyword == Keyword.VOID

    override fun close() {
        tokenizer.close()
        writer.close()
    }

    fun compile() {
        tokenizer.advance()
        compileClass()
    }

    private fun compileClass() {
        writeNonTerminal("class") {
            writeKeyword(Keyword.CLASS) { "'class' ←" }

            writeIdentifier { "'class' className ←" }

            writeSymbol('{') { "'class' className '{' ←" }

            while (isClassVarDec)
                compileClassVarDec()

            while (isSubroutineDec)
                compileSubroutine()

            writeSymbol('}') { "'class' className '{' classVarDec* subroutineDec* '}' ←" }
        }
    }

    private fun compileClassVarDec() {
        writeNonTerminal("classVarDec") {
            writeKeyword(classVarDecKeywords) { "('static' | 'field') ←" }

            compileType(void = false)

            writeIdentifier { "('static' | 'field') type varName ←" }

            while (tokenizer.symbol == ',') {
                writeSymbol(',') { "('static' | 'field') type varName (',' varName ←)*" }
                writeIdentifier { "('static' | 'field') type varName (',' ← varName)*" }
            }

            writeSymbol(';') { "('static' | 'field') type varName (',' varName)* ';' ←" }
        }
    }

    private fun compileSubroutine() {
        writeNonTerminal("subroutineDec") {
            writeKeyword(subroutineDecKeywords) { "('constructor' | 'function' | 'method') ←" }

            compileType(void = true)

            writeIdentifier { "('constructor' | 'function' | 'method') ('void' | type) subroutineName ←" }

            writeSymbol('(') { "('constructor' | 'function' | 'method') ('void' | type) subroutineName '(' ←" }

            compileParameterList()

            writeSymbol(')') { "('constructor' | 'function' | 'method') ('void' | type) subroutineName '(' parameterList ')' ←" }

            compileSubroutineBody()
        }
    }

    private fun compileParameterList() {
        writeNonTerminal("parameterList") {
            if (isBuiltInType || isIdentifier) {
                compileType(void = false)

                writeIdentifier { "type varName ←" }

                while (tokenizer.symbol == ',') {
                    writeSymbol(',') { "type varName (',' ← type varName)*" }
                    compileType(void = false)
                    writeIdentifier { "type varName (',' type varName ←)*" }
                }
            }
        }
    }

    private fun compileSubroutineBody() {
        writeNonTerminal("subroutineBody") {
            writeSymbol('{') { "'{' ←" }

            while (tokenizer.keyword == Keyword.VAR)
                compileVarDec()

            compileStatements()

            writeSymbol('}') { "'{' varDec* statements '}' ←" }
        }
    }

    private fun compileVarDec() {
        writeNonTerminal("varDec") {
            writeKeyword(Keyword.VAR) { "'var' ←" }

            compileType(void = false)

            writeIdentifier { "'var' type varName ←" }

            while (tokenizer.symbol == ',') {
                writeSymbol(',') { "'var' type varName (',' ← varName)*" }
                writeIdentifier { "'var' type varName (',' varName ←)*" }
            }

            writeSymbol(';') { "'var' type varName (',' varName)* ';' ←" }
        }
    }

    private fun compileStatements() {
        writeNonTerminal("statements") {
            loop@ while (true) {
                when (tokenizer.keyword) {
                    Keyword.LET -> compileLet()
                    Keyword.IF -> compileIf()
                    Keyword.WHILE -> compileWhile()
                    Keyword.DO -> compileDo()
                    Keyword.RETURN -> compileReturn()
                    else -> break@loop
                }
            }
        }
    }

    private fun compileLet() {
        writeNonTerminal("letStatement") {
            writeKeyword(Keyword.LET) { "'let' ←" }

            writeIdentifier { "'let' varName ←" }

            if (tokenizer.symbol == '[') {
                writeSymbol('[') { "'let' varName ('[' ← expression ']')?" }
                compileExpression()
                writeSymbol(']') { "'let' varName ('[' expression ']' ←)?" }
            }

            writeSymbol('=') { "'let' varName ('[' expression ']')? '=' ←" }

            compileExpression()

            writeSymbol(';') { "'let' varName ('[' expression ']')? '=' expression ';' ←" }
        }
    }

    private fun compileIf() {
        writeNonTerminal("ifStatement") {
            writeKeyword(Keyword.IF) { "'if' ←" }

            writeSymbol('(') { "'if' '(' ←" }

            compileExpression()

            writeSymbol(')') { "'if' '(' expression ')' ←" }

            writeSymbol('{') { "'if' '(' expression ')' '{' ←" }

            compileStatements()

            writeSymbol('}') { "'if' '(' expression ')' '{' statements '}' ←" }

            if (tokenizer.keyword == Keyword.ELSE) {
                writeKeyword(Keyword.ELSE) { "'if' '(' expression ')' '{' statements '}' ('else' ← '{' statements '}')?" }

                writeSymbol('{') { "'if' '(' expression ')' '{' statements '}' ('else' '{' ← statements '}')?" }

                compileStatements()

                writeSymbol('}') { "'if' '(' expression ')' '{' statements '}' ('else' '{' statements '}' ←)?" }
            }
        }
    }

    private fun compileWhile() {
        writeNonTerminal("whileStatement") {
            writeKeyword(Keyword.WHILE) { "'while'←" }

            writeSymbol('(') { "'while' '(' ←" }

            compileExpression()

            writeSymbol(')') { "'while' '(' expression ')' ←" }

            writeSymbol('{') { "'while' '(' expression ')' '{' ←" }

            compileStatements()

            writeSymbol('}') { "'while' '(' expression ')' '{' statements '}' ←" }
        }
    }

    private fun compileDo() {
        writeNonTerminal("doStatement") {
            writeKeyword(Keyword.DO) { "'do' ←" }

            compileSubroutineCall()

            writeSymbol(';') { "'do' subroutineCall ';' ←" }
        }
    }

    private fun compileReturn() {
        writeNonTerminal("returnStatement") {
            writeKeyword(Keyword.RETURN) { "'return' ←" }

            if (tokenizer.symbol != ';')
                compileExpression()

            writeSymbol(';') { "'return' expression? ';' ←" }
        }
    }

    private fun compileExpression() {
        writeNonTerminal("expression") {
            compileTerm()

            while (tokenizer.symbol in opSymbols) {
                writeSymbol(opSymbols) { "term (op ← term)*" }
                compileTerm()
            }
        }
    }

    private fun compileExpressionList() {
        writeNonTerminal("expressionList") {
            if (tokenizer.symbol != ')') {
                compileExpression()

                while (tokenizer.symbol == ',') {
                    writeSymbol(',') { "(expression (',' ← expression)*)?" }
                    compileExpression()
                }
            }
        }
    }

    private fun compileTerm() {
        writeNonTerminal("term") {
            when {
                isIntConst -> writeIntConst { "integerConstant" }
                isStringConst -> writeStringConst { "stringConstant" }
                isKeywordConstant -> writeKeyword(constantKeywords) { "keywordConstant" }
                isUnaryOpSymbols -> {
                    writeSymbol(unaryOpSymbols) { "unaryOp" }
                    compileTerm()
                }
                // TODO: fix
                else -> writeIdentifier { "test" }
            }
        }
    }

    private fun compileType(void: Boolean) {
        when {
            isBuiltInType -> writeKeyword(buildInTypeKeywords) { "type error (built in type)" }
            void && isVoid -> writeKeyword(Keyword.VOID) { "type error (void)" }
            else -> writeIdentifier { "type error" }
        }
    }

    private fun compileSubroutineCall() {
        writeIdentifier { "subroutineName | (className | varName) ←" }

        if (tokenizer.symbol == '.') {
            writeSymbol('.') { "(className | varName) '.' ←" }
            writeIdentifier { "(className | varName) '.' subroutineName ←" }
        }

        writeSymbol('(') { "subroutineName '(' ←" }

        compileExpressionList()

        writeSymbol(')') { "subroutineName '(' expressionList ')' ←" }
    }

    private fun writeln(line: String) {
        writer.write(" ".repeat(indentNum * 2))
        writer.write(line)
        writer.newLine()
    }

    private fun writeTerminal(tag: String, terminal: String) {
        writeln("<$tag> $terminal </$tag>")
        tokenizer.advance()
    }

    private fun writeNonTerminal(tag: String, block: () -> Any) {
        writeln("<$tag>")
        indentNum++
        block()
        indentNum--
        writeln("</$tag>")
    }

    private fun writeKeyword(expectedKeyword: Keyword, errorMessage: () -> Any) {
        check(tokenizer.keyword == expectedKeyword, errorMessage)
        writeTerminal("keyword", expectedKeyword.getLower())
    }

    private fun writeKeyword(expectedKeywords: List<Keyword>, errorMessage: () -> Any) {
        check(tokenizer.keyword in expectedKeywords, errorMessage)
        writeTerminal("keyword", tokenizer.keyword!!.getLower())
    }

    private fun writeSymbol(expectedSymbol: Char, errorMessage: () -> Any) {
        check(tokenizer.symbol == expectedSymbol, errorMessage)
        writeTerminal("symbol", expectedSymbol.toString())
    }

    private fun writeSymbol(expectedSymbols: List<Char>, errorMessage: () -> Any) {
        check(tokenizer.symbol in expectedSymbols, errorMessage)
        writeTerminal("symbol", tokenizer.symbol!!.toString())
    }

    private fun writeIdentifier(errorMessage: () -> Any) {
        checkNotNull(tokenizer.identifier, errorMessage)
        writeTerminal("identifier", tokenizer.identifier!!)
    }

    private fun writeIntConst(errorMessage: () -> Any) {
        checkNotNull(tokenizer.intVal, errorMessage)
        writeTerminal("integerConstant", tokenizer.intVal!!.toString())
    }

    private fun writeStringConst(errorMessage: () -> Any) {
        checkNotNull(tokenizer.stringVal, errorMessage)
        writeTerminal("stringConstant", tokenizer.stringVal!!)
    }

    companion object {
        private val classVarDecKeywords = listOf(Keyword.STATIC, Keyword.FIELD)
        private val subroutineDecKeywords = listOf(Keyword.CONSTRUCTOR, Keyword.FUNCTION, Keyword.METHOD)
        private val buildInTypeKeywords = listOf(Keyword.INT, Keyword.CHAR, Keyword.BOOLEAN)
        private val constantKeywords = listOf(Keyword.TRUE, Keyword.FALSE, Keyword.NULL, Keyword.THIS)
        private val opSymbols = listOf('+', '-', '*', '/', '&', '|', '<', '>', '=')
        private val unaryOpSymbols = listOf('-', '~')
    }
}