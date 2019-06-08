package jp.shiita.compiler

import org.junit.jupiter.api.Assertions.assertEquals
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File

object CompilationEngineSpec : Spek({
    listOf(
        "Average",
        "ComplexArrays",
        "ConvertToBin",
        "Pong",
        "Seven",
        "Square"
    ).forEach { dirName ->
        describe("CompilationEngine#compile $dirName") {
            File("src/test/resources/compiler/$dirName")
                .listFiles { f -> f.extension == "jack" }.toList()
                .forEach { jackFile ->
                    val cmpFile = File("${jackFile.path.substringBeforeLast(".")}_cmp.vm")
                    val compiledFile = File("${jackFile.path.substringBeforeLast(".")}.vm")

                    context("when ${jackFile.name} is compiled") {
                        before {
                            val tokenizer = JackTokenizer(jackFile.path)
                            val writer = VMWriter(compiledFile.path)
                            CompilationEngine(tokenizer, writer).use { it.compile() }
                        }

                        it("should equal ${cmpFile.name}") {
                            val compiled = compiledFile.readLines()
                            val cmp = cmpFile.readLines()

                            assertEquals(compiled, cmp)
                        }

                        after { compiledFile.delete() }
                    }
                }
        }
    }
})