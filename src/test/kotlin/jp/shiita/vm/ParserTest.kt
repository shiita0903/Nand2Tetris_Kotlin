package jp.shiita.vm

import org.junit.jupiter.api.Assertions.assertEquals
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File

object ParserSpec : Spek({
    describe("Parser#advance") {
        val file = File.createTempFile("tmp_", ".vm")

        context("when arithmetic command is parsed") {
            val arithmeticCommands = listOf("add", "sub", "neg", "eq", "gt", "lt", "and", "or", "not")

            arithmeticCommands.forEach { command ->
                it("when $command is parsed") {
                    file.writeText(command)
                    val parser = Parser(file.path)

                    parser.advance()

                    assertEquals(Parser.CommandType.ARITHMETIC, parser.commandType)
                    assertEquals(command, parser.arg1)
                    assertEquals(null, parser.arg2)
                }
            }
        }

        context("when push is parsed") {
            val segments = listOf("argument", "local", "static", "constant", "this", "that", "pointer", "temp")
            val indices = listOf(0, 1)

            segments.forEach { segment ->
                indices.forEach { index ->
                    it("when push $segment $index is parsed") {
                        file.writeText("push $segment $index")
                        val parser = Parser(file.path)

                        parser.advance()

                        assertEquals(Parser.CommandType.PUSH, parser.commandType)
                        assertEquals(segment, parser.arg1)
                        assertEquals(index, parser.arg2)
                    }
                }
            }
        }

        context("when pop is parsed") {
            val segments = listOf("argument", "local", "static", "constant", "this", "that", "pointer", "temp")
            val indices = listOf(0, 1)

            segments.forEach { segment ->
                indices.forEach { index ->
                    it("when pop $segment $index is parsed") {
                        file.writeText("pop $segment $index")
                        val parser = Parser(file.path)

                        parser.advance()

                        assertEquals(Parser.CommandType.POP, parser.commandType)
                        assertEquals(segment, parser.arg1)
                        assertEquals(index, parser.arg2)
                    }
                }
            }
        }

        context("when label is parsed") {
            val label = "azAZ10_.:"

            it("when label $label is parsed") {
                file.writeText("label $label")
                val parser = Parser(file.path)

                parser.advance()

                assertEquals(Parser.CommandType.LABEL, parser.commandType)
                assertEquals(label, parser.arg1)
                assertEquals(null, parser.arg2)
            }
        }

        context("when goto is parsed") {
            val label = "azAZ10_.:"

            it("when goto $label is parsed") {
                file.writeText("goto $label")
                val parser = Parser(file.path)

                parser.advance()

                assertEquals(Parser.CommandType.GOTO, parser.commandType)
                assertEquals(label, parser.arg1)
                assertEquals(null, parser.arg2)
            }
        }

        context("when if-goto is parsed") {
            val label = "azAZ10_.:"

            it("when if-goto $label is parsed") {
                file.writeText("if-goto $label")
                val parser = Parser(file.path)

                parser.advance()

                assertEquals(Parser.CommandType.IF, parser.commandType)
                assertEquals(label, parser.arg1)
                assertEquals(null, parser.arg2)
            }
        }

        context("when function is parsed") {
            val functionName = "azAZ10_.:"
            val nums = listOf(0, 1, 10)

            nums.forEach { num ->
                it("when function $functionName $num is parsed") {
                    file.writeText("function $functionName $num")
                    val parser = Parser(file.path)

                    parser.advance()

                    assertEquals(Parser.CommandType.FUNCTION, parser.commandType)
                    assertEquals(functionName, parser.arg1)
                    assertEquals(num, parser.arg2)
                }
            }
        }

        context("when call is parsed") {
            val functionName = "azAZ10_.:"
            val nums = listOf(0, 1, 10)

            nums.forEach { num ->
                it("when call $functionName $num is parsed") {
                    file.writeText("call $functionName $num")
                    val parser = Parser(file.path)

                    parser.advance()

                    assertEquals(Parser.CommandType.CALL, parser.commandType)
                    assertEquals(functionName, parser.arg1)
                    assertEquals(num, parser.arg2)
                }
            }
        }

        file.deleteOnExit()
    }
})