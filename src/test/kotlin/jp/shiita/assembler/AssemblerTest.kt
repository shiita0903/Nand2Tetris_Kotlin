package jp.shiita.assembler

import org.junit.jupiter.api.Assertions.assertEquals
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File
import java.io.FileReader

object AssemblerSpec : Spek({
    describe("Assembler#assemble(without symbol)") {
        listOf("Add.asm", "MaxL.asm", "PongL.asm", "RectL.asm")
            .map { it to "src/test/resource/$it" }
            .forEach { (asmFile, asmPath) ->
                val cmpFile = asmFile.replace(".asm", ".cmp")
                val hackPath = asmPath.replace(".asm", ".hack")
                val cmpPath = asmPath.replace(".asm", ".cmp")

                context("when $asmFile is assembled") {
                    before { Assembler(asmPath, false).assemble() }

                    it ("should equal $cmpFile") {
                        val hack = FileReader(hackPath).use { it.readText() }
                        val cmp = FileReader(cmpPath).use { it.readText() }
                        assertEquals(hack, cmp)
                    }

                    after { File(hackPath).delete() }
                }
            }
    }

    describe("Assembler#assemble(with symbol)") {
        listOf("Max.asm", "Pong.asm", "Rect.asm")
            .map { it to "src/test/resource/$it" }
            .forEach { (asmFile, asmPath) ->
                val cmpFile = asmFile.replace(".asm", ".cmp")
                val hackPath = asmPath.replace(".asm", ".hack")
                val cmpPath = asmPath.replace(".asm", ".cmp")

                context("when $asmFile is assembled") {
                    before { Assembler(asmPath, false).assemble() }

                    it ("should equal $cmpFile") {
                        val hack = FileReader(hackPath).use { it.readText() }
                        val cmp = FileReader(cmpPath).use { it.readText() }
                        assertEquals(hack, cmp)
                    }

                    after { File(hackPath).delete() }
                }
            }
    }
})