package jp.shiita.compiler

import org.junit.jupiter.api.Assertions.assertEquals
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File

object TokenWriterSpec : Spek({
    describe("TokenWriter#tokenize") {
        listOf("Square/Main.jack", "Square/Square.jack", "Square/SquareGame.jack")
            .map { File("src/test/resources/compiler/$it") }
            .forEach { jackFile ->
                val cmpFile = File("${jackFile.path.substringBeforeLast(".")}T_cmp.xml")
                val tokenizedFile = File("${jackFile.path.substringBeforeLast(".")}T.xml")

                context("when ${jackFile.name} is tokenized") {
                    before {
                        val tokenizer = JackTokenizer(jackFile.path)
                        TokenWriter(tokenizer, tokenizedFile.path).use { it.tokenize() }
                    }

                    it("should equal ${cmpFile.name}") {
                        val tokenized = tokenizedFile.readLines()
                        val cmp = cmpFile.readLines()

                        assertEquals(tokenized, cmp)
                    }

                    after { tokenizedFile.delete() }
                }
            }
    }
})