package jp.shiita.assembler

import jp.shiita.extensions.replaced
import org.junit.jupiter.api.Assertions.assertEquals
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

object AssemblerSpec : Spek({
    describe("Assembler#assemble(without symbol)") {
        listOf("Add.asm", "MaxL.asm", "PongL.asm", "RectL.asm")
            .map { File("src/test/resources/assembler/$it") }
            .forEach { asmFile ->
                val cmpFile = asmFile.replaced("cmp")
                val hackFile = asmFile.replaced("hack")

                context("when ${asmFile.name} is assembled") {
                    before {
                        val parser = Parser(asmFile.path, false)
                        val writer = BufferedWriter(FileWriter(hackFile.path))
                        Assembler(parser, writer, false).use { it.assemble() }
                    }

                    it("should equal ${cmpFile.name}") {
                        val hack = hackFile.readText()
                        val cmp = cmpFile.readText()
                        assertEquals(hack, cmp)
                    }

                    after { hackFile.delete() }
                }
            }
    }

    describe("Assembler#assemble(with symbol)") {
        listOf("Max.asm", "Pong.asm", "Rect.asm")
            .map { File("src/test/resources/assembler/$it") }
            .forEach { asmFile ->
                val cmpFile = asmFile.replaced("cmp")
                val hackFile = asmFile.replaced("hack")

                context("when ${asmFile.name} is assembled") {
                    before {
                        val parser = Parser(asmFile.path, false)
                        val writer = BufferedWriter(FileWriter(hackFile.path))
                        Assembler(parser, writer, false).use { it.assemble() }
                    }

                    it("should equal ${cmpFile.name}") {
                        val hack = hackFile.readText()
                        val cmp = cmpFile.readText()
                        assertEquals(hack, cmp)
                    }

                    after { hackFile.delete() }
                }
            }
    }
})