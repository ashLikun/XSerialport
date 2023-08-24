package com.ashlikun.serialport.handle

import java.io.InputStream

interface AbsSerialReadHandle {
    fun execute(ism: InputStream): ByteArray?
}