package com.ashlikun.serialport.handle

import android.os.SystemClock
import java.io.IOException
import java.io.InputStream

/**
 * 延迟的方式读取
 */
class DelaySerialReadHandle(var delay: Long = 20) : AbsSerialReadHandle {
    override fun execute(ism: InputStream): ByteArray? {
        try {
            val available = ism.available()
            if (available > 0) {
                val buffer = ByteArray(available)
                val size = ism.read(buffer)
                if (size > 0) {
                    return buffer
                }
            } else {
                SystemClock.sleep(delay)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }
}