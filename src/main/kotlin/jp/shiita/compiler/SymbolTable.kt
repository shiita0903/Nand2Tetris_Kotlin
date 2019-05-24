package jp.shiita.compiler

class SymbolTable {
    private val classTable = hashMapOf<String, Triple<Int, String, Kind>>()
    private val subroutineTable = hashMapOf<String, Triple<Int, String, Kind>>()
    private var staticCount = 0
    private var fieldCount = 0
    private var argumentCount = 0
    private var varCount = 0

    fun startSubroutine() {
        subroutineTable.clear()
        argumentCount = 0
        varCount = 0
    }

    fun define(name: String, type: String, kind: Kind) = when (kind) {
        Kind.STATIC -> classTable[name] = Triple(staticCount++, type, kind)
        Kind.FIELD -> classTable[name] = Triple(fieldCount++, type, kind)
        Kind.ARGUMENT -> subroutineTable[name] = Triple(argumentCount++, type, kind)
        Kind.VAR -> subroutineTable[name] = Triple(varCount++, type, kind)
        else -> error("invalid Kind : \"$kind\"")
    }

    fun variableCount(kind: Kind) = when (kind) {
        Kind.STATIC -> staticCount
        Kind.FIELD -> fieldCount
        Kind.ARGUMENT -> argumentCount
        Kind.VAR -> varCount
        else -> error("invalid Kind : \"$kind\"")
    }

    fun indexOf(name: String): Int = searchTable(name)?.first ?: error("not found \"$name\"")

    fun typeOf(name: String): String = searchTable(name)?.second ?: error("not found \"$name\"")

    fun kindOf(name: String): Kind = searchTable(name)?.third ?: Kind.NONE

    private fun searchTable(name: String): Triple<Int, String, Kind>? =
        subroutineTable.getOrElse(name) { classTable[name] }

    enum class Kind { STATIC, FIELD, ARGUMENT, VAR, NONE }
}