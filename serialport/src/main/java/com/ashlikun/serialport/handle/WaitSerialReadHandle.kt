package com.ashlikun.serialport.handle

import java.io.IOException
import java.io.InputStream

/**
 * 阻塞的方式读取
 */
class WaitSerialReadHandle(var initSize: Int = 102400) : AbsSerialReadHandle {
    override fun execute(ism: InputStream): ByteArray? {
        try {
            val buffer = ByteArray(initSize)
            val size = ism.read(buffer)
            if (size > 0) {
                return buffer.copyOf(size)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }
}