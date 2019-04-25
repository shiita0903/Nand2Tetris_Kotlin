package jp.shiita.assembler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object CodeSpec : Spek({
    describe("Code#comp") {
        val mnemonics = listOf(
            "0"   to 0b0101010,
            "1"   to 0b0111111,
            "-1"  to 0b0111010,
            "D"   to 0b0001100,
            "A"   to 0b0110000,
            "!D"  to 0b0001101,
            "!A"  to 0b0110001,
            "-D"  to 0b0001111,
            "-A"  to 0b0110011,
            "D+1" to 0b0011111,
            "A+1" to 0b0110111,
            "D-1" to 0b0001110,
            "A-1" to 0b0110010,
            "D+A" to 0b0000010,
            "D-A" to 0b0010011,
            "A-D" to 0b0000111,
            "D&A" to 0b0000000,
            "D|A" to 0b0010101,
            "M"   to 0b1110000,
            "!M"  to 0b1110001,
            "-M"  to 0b1110011,
            "M+1" to 0b1110111,
            "M-1" to 0b1110010,
            "D+M" to 0b1000010,
            "D-M" to 0b1010011,
            "M-D" to 0b1000111,
            "D&M" to 0b1000000,
            "D|M" to 0b1010101)

        mnemonics.forEach { (mnemonic, expected) ->
            context("when mnemonic is $mnemonic") {
                it("should return 0b${expected.toString(2).padStart(7, '0')}") {
                    assertEquals(expected, Code.comp(mnemonic))
                }
            }
        }

        val mnemonic = "D*A"
        context("when mnemonic is $mnemonic") {
            it("should throw Exception") { assertThrows<Exception> { Code.dest(mnemonic) } }
        }
    }

    describe("Code#dest") {
        val mnemonics = listOf(
            "null" to 0b000,
            "M"    to 0b001,
            "D"    to 0b010,
            "MD"   to 0b011,
            "DM"   to 0b011,
            "A"    to 0b100,
            "AM"   to 0b101,
            "MA"   to 0b101,
            "AD"   to 0b110,
            "DA"   to 0b110,
            "AMD"  to 0b111,
            "ADM"  to 0b111,
            "MAD"  to 0b111,
            "MDA"  to 0b111,
            "DAM"  to 0b111,
            "DMA"  to 0b111)

        mnemonics.forEach { (mnemonic, expected) ->
            context("when mnemonic is $mnemonic") {
                it("should return 0b${expected.toString(2).padStart(3, '0')}") {
                    assertEquals(expected, Code.dest(mnemonic))
                }
            }
        }

        val mnemonic = "AMX"
        context("when mnemonic is $mnemonic") {
            it("should throw Exception") { assertThrows<Exception> { Code.dest(mnemonic) } }
        }
    }

    describe("Code#jump") {
        val mnemonics = listOf(
            "null" to 0b000,
            "JGT"  to 0b001,
            "JEQ"  to 0b010,
            "JGE"  to 0b011,
            "JLT"  to 0b100,
            "JNE"  to 0b101,
            "JLE"  to 0b110,
            "JMP"  to 0b111)

        mnemonics.forEach { (mnemonic, expected) ->
            context("when mnemonic is $mnemonic") {
                it("should return 0b${expected.toString(2).padStart(3, '0')}") {
                    assertEquals(expected, Code.jump(mnemonic))
                }
            }
        }

        val mnemonic = "JNN"
        context("when mnemonic is $mnemonic") {
            it("should throw Exception") { assertThrows<Exception> { Code.dest(mnemonic) } }
        }
    }
})