package com.ex.serialport

import java.util.Locale

object ByteUtil {
    fun isOdd(num: Int): Int {
        return num and 0x1
    }

    fun HexToInt(inHex: String): Int {
        return inHex.toInt(16)
    }

    fun HexToByte(inHex: String): Byte {
        return inHex.toInt(16).toByte()
    }

    fun Byte2Hex(inByte: Byte): String {
        return String.format("%02x", *arrayOf<Any>(inByte)).uppercase(Locale.getDefault())
    }

    fun ByteArrToHex(inBytArr: ByteArray): String {
        val strBuilder = StringBuilder()
        val j = inBytArr.size
        for (i in 0 until j) {
            strBuilder.append(Byte2Hex(java.lang.Byte.valueOf(inBytArr[i])))
            strBuilder.append("")
        }
        return strBuilder.toString()
    }

    fun ByteArrToHex(inBytArr: ByteArray, offset: Int, byteCount: Int): String {
        val strBuilder = StringBuilder()
        for (i in offset until byteCount) {
            strBuilder.append(Byte2Hex(java.lang.Byte.valueOf(inBytArr[i])))
        }
        return strBuilder.toString()
    }

    fun hexStr2Bytes(str: String?): ByteArray {
        if (str.isNullOrEmpty()) {
            return ByteArray(0)
        }
        //替换空格
        val str = str.replace(" ", "")
        val byteArray = ByteArray(str.length / 2)
        for (i in byteArray.indices) {
            val subStr = str.substring(2 * i, 2 * i + 2)
            byteArray[i] = subStr.toInt(16).toByte()
        }
        return byteArray
    }
}