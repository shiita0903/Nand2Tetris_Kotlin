package jp.shiita.compiler

import org.junit.jupiter.api.Assertions.assertEquals
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File

object CompilationEngineSpec : Spek({
    describe("CompilationEngine#compile(without expression)") {
        listOf(
            "ExpressionLessSquare/Main.jack",
            "ExpressionLessSquare/Square.jack",
            "ExpressionLessSquare/SquareGame.jack"
        )
            .map { File("src/test/resources/compiler/$it") }
            .forEach { jackFile ->
                val cmpFile = File("${jackFile.path.substringBeforeLast(".")}_cmp.xml")
                val parsedFile = File("${jackFile.path.substringBeforeLast(".")}.xml")

                context("when ${jackFile.name} is parsed") {
                    before {
                        val tokenizer = JackTokenizer(jackFile.path)
                        CompilationEngine(tokenizer, parsedFile.path).use { it.compile() }
                    }

                    it("should equal ${cmpFile.name}") {
                        val parsed = parsedFile.readLines()
                        val cmp = cmpFile.readLines()

                        assertEquals(parsed, cmp)
                    }

                    after { parsedFile.delete() }
                }
            }
    }

    describe("CompilationEngine#compile(with expression)") {
        listOf(
            "ArrayTest/Main.jack",
            "Square/Main.jack",
            "Square/Square.jack",
            "Square/SquareGame.jack"
        )
            .map { File("src/test/resources/compiler/$it") }
            .forEach { jackFile ->
                val cmpFile = File("${jackFile.path.substringBeforeLast(".")}_cmp.xml")
                val parsedFile = File("${jackFile.path.substringBeforeLast(".")}.xml")

                context("when ${jackFile.name} is parsed") {
                    before {
                        val tokenizer = JackTokenizer(jackFile.path)
                        CompilationEngine(tokenizer, parsedFile.path).use { it.compile() }
                    }

                    it("should equal ${cmpFile.name}") {
                        val parsed = parsedFile.readLines()
                        val cmp = cmpFile.readLines()

                        assertEquals(parsed, cmp)
                    }

                    after { parsedFile.delete() }
                }
            }
    }
})