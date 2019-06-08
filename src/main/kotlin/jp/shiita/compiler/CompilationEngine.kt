package jp.shiita.compiler

import jp.shiita.compiler.JackTokenizer.Keyword
import jp.shiita.compiler.JackTokenizer.TokenType
import java.io.Closeable

class CompilationEngine(
    private val tokenizer: JackTokenizer,
    private val writer: VMWriter
) : Closeable {
    private val symbolTable = SymbolTable()
    private lateinit var className: String
    private var ifCount = 0
    private var whileCount = 0

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
    private val isVar
        get() = tokenizer.keyword == Keyword.VAR
    private val isElse
        get() = tokenizer.keyword == Keyword.ELSE

    override fun close() {
        tokenizer.close()
        writer.close()
    }

    fun compile() {
        tokenizer.advance()
        compileClass()
    }

    private fun compileClass() {
        checkKeyword(Keyword.CLASS) { "'class' ←" }

        className = checkIdentifier { "'class' className ←" }

        checkSymbol('{') { "'class' className '{' ←" }

        while (isClassVarDec)
            compileClassVarDec()

        while (isSubroutineDec)
            compileSubroutine()

        checkSymbol('}') { "'class' className '{' classVarDec* subroutineDec* '}' ←" }
    }

    private fun compileClassVarDec() {
        val isStatic = checkKeyword(classVarDecKeywords) { "('static' | 'field') ←" } == Keyword.STATIC

        val type = checkType(void = false)

        val varNames = mutableListOf(checkIdentifier { "('static' | 'field') type varName ←" })

        while (tokenizer.symbol == ',') {
            checkSymbol(',') { "('static' | 'field') type varName (',' ← varName)*" }
            varNames.add(checkIdentifier { "('static' | 'field') type varName (',' varName ←)*" })
        }

        checkSymbol(';') { "('static' | 'field') type varName (',' varName)* ';' ←" }

        varNames.forEach { name ->
            symbolTable.define(
                name,
                type,
                if (isStatic) SymbolTable.Kind.STATIC else SymbolTable.Kind.FIELD
            )
        }
    }

    private fun compileSubroutine() {
        ifCount = 0
        whileCount = 0
        symbolTable.startSubroutine()

        val subroutineType = checkKeyword(subroutineDecKeywords) { "('constructor' | 'function' | 'method') ←" }
        if (subroutineType == Keyword.METHOD) {
            symbolTable.define("this", className, SymbolTable.Kind.ARGUMENT)
        }

        val returnType = checkType(void = true)

        val subroutineName =
            checkIdentifier { "('constructor' | 'function' | 'method') ('void' | type) subroutineName ←" }

        checkSymbol('(') { "('constructor' | 'function' | 'method') ('void' | type) subroutineName '(' ←" }

        compileParameterList()

        checkSymbol(')') { "('constructor' | 'function' | 'method') ('void' | type) subroutineName '(' parameterList ')' ←" }

        compileSubroutineBody(subroutineType, returnType, subroutineName)
    }

    private fun compileParameterList() {
        if (isBuiltInType || isIdentifier) {
            compileParameter()

            while (tokenizer.symbol == ',') {
                checkSymbol(',') { "type varName (',' ← type varName)*" }
                compileParameter()
            }
        }
    }

    private fun compileParameter() {
        val type = checkType(void = false)
        val varName = checkIdentifier { "type varName ←" }
        symbolTable.define(varName, type, SymbolTable.Kind.ARGUMENT)
    }

    private fun compileSubroutineBody(subroutineType: Keyword, returnType: String, subroutineName: String) {
        checkSymbol('{') { "'{' ←" }

        val localCount = sequence {
            while (isVar)
                yield(compileVarDec())
        }.sum()
        writer.writeFunction("$className.$subroutineName", localCount)

        when (subroutineType) {
            Keyword.CONSTRUCTOR -> {
                // memory allocation for itself
                writer.writePush(VMWriter.Segment.CONSTANT, symbolTable.variableCount(SymbolTable.Kind.FIELD))
                writer.writeCall("Memory.alloc", 1)
                writer.writePop(VMWriter.Segment.POINTER, 0)
            }
            Keyword.METHOD -> {
                // setup this segment
                writer.writePush(VMWriter.Segment.ARGUMENT, 0)
                writer.writePop(VMWriter.Segment.POINTER, 0)
            }
        }

        compileStatements()

        checkSymbol('}') { "'{' varDec* statements '}' ←" }
    }

    private fun compileVarDec(): Int {
        checkKeyword(Keyword.VAR) { "'var' ←" }

        val type = checkType(void = false)

        val varNames = mutableListOf(checkIdentifier { "'var' type varName ←" })

        while (tokenizer.symbol == ',') {
            checkSymbol(',') { "'var' type varName (',' ← varName)*" }
            varNames.add(checkIdentifier { "'var' type varName (',' varName ←)*" })
        }

        checkSymbol(';') { "'var' type varName (',' varName)* ';' ←" }

        varNames.forEach { name -> symbolTable.define(name, type, SymbolTable.Kind.VAR) }
        return varNames.size
    }

    private fun compileStatements() {
        while (true) {
            when (tokenizer.keyword) {
                Keyword.LET -> compileLet()
                Keyword.IF -> compileIf()
                Keyword.WHILE -> compileWhile()
                Keyword.DO -> compileDo()
                Keyword.RETURN -> compileReturn()
                else -> return
            }
        }
    }

    private fun compileLet() {
        checkKeyword(Keyword.LET) { "'let' ←" }

        val varName = checkIdentifier { "'let' varName ←" }
        val index = symbolTable.indexOf(varName)
        val kind = symbolTable.kindOf(varName)
        val segment = kindToSegment(kind) { "not defined symbol : \"$varName\"" }

        val isArray = tokenizer.symbol == '['
        if (isArray) {
            checkSymbol('[') { "'let' varName ('[' ← expression ']')?" }
            compileExpression()
            checkSymbol(']') { "'let' varName ('[' expression ']' ←)?" }

            writer.writePush(segment, index)
            writer.writeArithmetic(VMWriter.Command.ADD)
        }

        checkSymbol('=') { "'let' varName ('[' expression ']')? '=' ←" }

        compileExpression()

        if (isArray) {
            writer.writePop(VMWriter.Segment.TEMP, 0)
            writer.writePop(VMWriter.Segment.POINTER, 1)
            writer.writePush(VMWriter.Segment.TEMP, 0)
            writer.writePop(VMWriter.Segment.THAT, 0)
        } else {
            writer.writePop(segment, index)
        }

        checkSymbol(';') { "'let' varName ('[' expression ']')? '=' expression ';' ←" }
    }

    private fun compileIf() {
        val trueLabel = "IF_TRUE$ifCount"
        val falseLabel = "IF_FALSE$ifCount"
        val endLabel = "IF_END$ifCount"
        ifCount++

        checkKeyword(Keyword.IF) { "'if' ←" }

        checkSymbol('(') { "'if' '(' ←" }

        compileExpression()
        writer.writeIf(trueLabel)
        writer.writeGoto(falseLabel)
        writer.writeLabel(trueLabel)

        checkSymbol(')') { "'if' '(' expression ')' ←" }

        checkSymbol('{') { "'if' '(' expression ')' '{' ←" }

        compileStatements()

        checkSymbol('}') { "'if' '(' expression ')' '{' statements '}' ←" }

        if (isElse) {
            writer.writeGoto(endLabel)
            writer.writeLabel(falseLabel)

            checkKeyword(Keyword.ELSE) { "'if' '(' expression ')' '{' statements '}' ('else' ← '{' statements '}')?" }

            checkSymbol('{') { "'if' '(' expression ')' '{' statements '}' ('else' '{' ← statements '}')?" }

            compileStatements()

            checkSymbol('}') { "'if' '(' expression ')' '{' statements '}' ('else' '{' statements '}' ←)?" }

            writer.writeLabel(endLabel)
        } else {
            writer.writeLabel(falseLabel)
        }
    }

    private fun compileWhile() {
        val expLabel = "WHILE_EXP$whileCount"
        val endLabel = "WHILE_END$whileCount"
        whileCount++

        checkKeyword(Keyword.WHILE) { "'while' ←" }

        checkSymbol('(') { "'while' '(' ←" }

        writer.writeLabel(expLabel)
        compileExpression()
        writer.writeArithmetic(VMWriter.Command.NOT)
        writer.writeIf(endLabel)

        checkSymbol(')') { "'while' '(' expression ')' ←" }

        checkSymbol('{') { "'while' '(' expression ')' '{' ←" }

        compileStatements()
        writer.writeGoto(expLabel)
        writer.writeLabel(endLabel)

        checkSymbol('}') { "'while' '(' expression ')' '{' statements '}' ←" }
    }

    private fun compileDo() {
        checkKeyword(Keyword.DO) { "'do' ←" }

        val name = checkIdentifier { "subroutineName | className | varName" }
        compileSubroutineCall(name)
        writer.writePop(VMWriter.Segment.TEMP, 0)

        checkSymbol(';') { "'do' subroutineCall ';' ←" }
    }

    private fun compileReturn() {
        checkKeyword(Keyword.RETURN) { "'return' ←" }

        if (tokenizer.symbol != ';') compileExpression()
        else writer.writePush(VMWriter.Segment.CONSTANT, 0)
        writer.writeReturn()

        checkSymbol(';') { "'return' expression? ';' ←" }
    }

    private fun compileExpression() {
        compileTerm()

        while (tokenizer.symbol in opSymbols) {
            val op = checkSymbol(opSymbols) { "term (op ← term)*" }
            val command = VMWriter.opCommandMap[op]

            compileTerm()

            when {
                command != null -> writer.writeArithmetic(command)
                op == '*' -> writer.writeCall("Math.multiply", 2)
                op == '/' -> writer.writeCall("Math.divide", 2)
            }
        }
    }

    private fun compileExpressionList(): Int {
        var argCount = 0

        if (tokenizer.symbol != ')') {
            argCount++
            compileExpression()

            while (tokenizer.symbol == ',') {
                checkSymbol(',') { "(expression (',' ← expression)*)?" }
                argCount++
                compileExpression()
            }
        }

        return argCount
    }

    private fun compileTerm() {
        when {
            isIntConst -> {
                val intVal = checkIntConst { "integerConstant" }
                writer.writePush(VMWriter.Segment.CONSTANT, intVal)
            }
            isStringConst -> {
                val stringVal = checkStringConst { "stringConstant" }
                writer.writePush(VMWriter.Segment.CONSTANT, stringVal.length)
                writer.writeCall("String.new", 1)
                stringVal.forEach { c ->
                    writer.writePush(VMWriter.Segment.CONSTANT, c.toInt())
                    writer.writeCall("String.appendChar", 2)    // String object is already pushed
                }
            }
            isKeywordConstant -> {
                when (tokenizer.keyword) {
                    Keyword.TRUE -> {
                        writer.writePush(VMWriter.Segment.CONSTANT, 0)
                        writer.writeArithmetic(VMWriter.Command.NOT)
                    }
                    Keyword.FALSE -> writer.writePush(VMWriter.Segment.CONSTANT, 0)
                    Keyword.NULL -> writer.writePush(VMWriter.Segment.CONSTANT, 0)
                    Keyword.THIS -> writer.writePush(VMWriter.Segment.POINTER, 0)
                    else -> error("invalid Keyword \"${tokenizer.keyword}\"")
                }
                tokenizer.advance()
            }
            tokenizer.symbol == '(' -> {
                checkSymbol('(') { "'(' ←" }
                compileExpression()
                checkSymbol(')') { "'(' expression ')' ←" }
            }
            isUnaryOpSymbols -> {
                val unaryOp = checkSymbol(unaryOpSymbols) { "unaryOp" }
                compileTerm()
                writer.writeArithmetic(if (unaryOp == '-') VMWriter.Command.NEG else VMWriter.Command.NOT)
            }
            else -> {
                val name = checkIdentifier { "varName | subroutineName | className" }
                when (tokenizer.symbol) {
                    '(', '.' -> compileSubroutineCall(name)
                    else -> {
                        val isArray = tokenizer.symbol == '['

                        if (isArray) {
                            checkSymbol('[') { "varName '[' ←" }
                            compileExpression()
                            checkSymbol(']') { "varName '[' expression ']' ←" }
                        }

                        val kind = symbolTable.kindOf(name)
                        val index = symbolTable.indexOf(name)
                        val segment = kindToSegment(kind) { "not defined symbol : \"$name\"" }
                        writer.writePush(segment, index)

                        if (isArray) {
                            writer.writeArithmetic(VMWriter.Command.ADD)
                            writer.writePop(VMWriter.Segment.POINTER, 1)
                            writer.writePush(VMWriter.Segment.THAT, 0)
                        }
                    }
                }
            }
        }
    }

    private fun compileSubroutineCall(name: String) {
        var isMethodCall = false
        val subroutineName = if (tokenizer.symbol == '.') {
            checkSymbol('.') { "(className | varName) '.' ←" }
            val identifier = checkIdentifier { "(className | varName) '.' subroutineName ←" }

            if (name[0].isUpperCase()) "$name.$identifier"
            else {
                isMethodCall = true
                "${symbolTable.typeOf(name)}.$identifier"
            }
        } else {
            isMethodCall = true
            "$className.$name"
        }

        checkSymbol('(') { "subroutineName '(' ←" }

        val argCount = if (isMethodCall) {
            val kind = symbolTable.kindOf(name)
            if (kind == SymbolTable.Kind.NONE) {
                writer.writePush(VMWriter.Segment.POINTER, 0)
            } else {
                val index = symbolTable.indexOf(name)
                val segment = kindToSegment(kind) { "not defined symbol : \"$name\"" }
                writer.writePush(segment, index)
            }

            compileExpressionList() + 1
        } else {
            compileExpressionList()
        }
        writer.writeCall(subroutineName, argCount)

        checkSymbol(')') { "subroutineName '(' expressionList ')' ←" }
    }

    private fun checkAdvance(value: Boolean, errorMessage: () -> Any) {
        check(value, errorMessage)
        tokenizer.advance()
    }

    private fun checkKeyword(expectedKeyword: Keyword, errorMessage: () -> Any): Keyword {
        val keyword = tokenizer.keyword
        checkAdvance(tokenizer.keyword == expectedKeyword, errorMessage)
        return keyword!!
    }

    private fun checkKeyword(expectedKeywords: List<Keyword>, errorMessage: () -> Any): Keyword {
        val keyword = tokenizer.keyword
        checkAdvance(keyword in expectedKeywords, errorMessage)
        return keyword!!
    }

    private fun checkSymbol(expectedSymbol: Char, errorMessage: () -> Any) =
        checkAdvance(tokenizer.symbol == expectedSymbol, errorMessage)

    private fun checkSymbol(expectedSymbols: List<Char>, errorMessage: () -> Any): Char {
        val symbol = tokenizer.symbol
        checkAdvance(symbol in expectedSymbols, errorMessage)
        return symbol!!
    }

    private fun checkIdentifier(errorMessage: () -> Any): String {
        val identifier = tokenizer.identifier
        checkNotNull(identifier, errorMessage)
        tokenizer.advance()
        return identifier
    }

    private fun checkIntConst(errorMessage: () -> Any): Int {
        val intVal = tokenizer.intVal
        checkNotNull(intVal, errorMessage)
        tokenizer.advance()
        return intVal
    }

    private fun checkStringConst(errorMessage: () -> Any): String {
        val stringVal = tokenizer.stringVal
        checkNotNull(stringVal, errorMessage)
        tokenizer.advance()
        return stringVal
    }

    private fun checkType(void: Boolean): String = when {
        isBuiltInType -> checkKeyword(buildInTypeKeywords) { "type error (built in type)" }.getLower()
        void && isVoid -> checkKeyword(Keyword.VOID) { "type error (void)" }.getLower()
        else -> checkIdentifier { "type error" }
    }

    private fun kindToSegment(kind: SymbolTable.Kind, errorMessage: () -> Any): VMWriter.Segment = when (kind) {
        SymbolTable.Kind.STATIC -> VMWriter.Segment.STATIC
        SymbolTable.Kind.FIELD -> VMWriter.Segment.THIS
        SymbolTable.Kind.ARGUMENT -> VMWriter.Segment.ARGUMENT
        SymbolTable.Kind.VAR -> VMWriter.Segment.LOCAL
        else -> error(errorMessage)
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