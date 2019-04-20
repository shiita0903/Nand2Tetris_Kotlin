package jp.shiita.assembler

object Code {
    fun comp(mnemonic: String): Int = when (mnemonic) {
        // a == 0
        "0"   -> 0b0101010
        "1"   -> 0b0111111
        "-1"  -> 0b0111010
        "D"   -> 0b0001100
        "A"   -> 0b0110000
        "!D"  -> 0b0001101
        "!A"  -> 0b0110001
        "-D"  -> 0b0001111
        "-A"  -> 0b0110011
        "D+1" -> 0b0011111
        "A+1" -> 0b0110111
        "D-1" -> 0b0001110
        "A-1" -> 0b0110010
        "D+A" -> 0b0000010
        "D-A" -> 0b0010011
        "A-D" -> 0b0000111
        "D&A" -> 0b0000000
        "D|A" -> 0b0010101
        // a == 1
        "M"   -> 0b1110000
        "!M"  -> 0b1110001
        "-M"  -> 0b1110011
        "M+1" -> 0b1110111
        "M-1" -> 0b1110010
        "D+M" -> 0b1000010
        "D-M" -> 0b1010011
        "M-D" -> 0b1000111
        "D&M" -> 0b1000000
        "D|M" -> 0b1010101
        else  -> error("invalid mnemonic : \"$mnemonic\"")
    }

    fun dest(mnemonic: String): Int {
        require(mnemonic == "null" || mnemonic.groupBy { it }.keys.all { it in "AMD" }) {
            "invalid mnemonic : \"$mnemonic\""
        }

        var ret = 0
        if ('A' in mnemonic) ret = ret or (1 shl 2)
        if ('D' in mnemonic) ret = ret or (1 shl 1)
        if ('M' in mnemonic) ret = ret or 1
        return ret
    }

    fun jump(mnemonic: String): Int = when (mnemonic) {
        "null" -> 0b000
        "JGT"  -> 0b001
        "JEQ"  -> 0b010
        "JGE"  -> 0b011
        "JLT"  -> 0b100
        "JNE"  -> 0b101
        "JLE"  -> 0b110
        "JMP"  -> 0b111
        else   -> error("invalid mnemonic : \"$mnemonic\"")
    }
}