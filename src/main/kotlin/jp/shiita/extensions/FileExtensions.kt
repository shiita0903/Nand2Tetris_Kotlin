package jp.shiita.extensions

import java.io.File

fun File.replaced(extension: String): File =
    if (isFile && '.' in name)
        File("${path.substringBeforeLast('.')}.$extension")
    else
        error("invalid file name : \"$name\"")
