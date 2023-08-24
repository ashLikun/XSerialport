package com.ashlikun.serialport

import android.util.Log
import android_serialport_api.SerialPort
import com.ashlikun.serialport.handle.AbsSerialReadHandle
import com.ashlikun.serialport.handle.WaitSerialReadHandle
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

typealias SerialOnDataReceived = (ByteArray) -> Unit

class SerialHelper(
    var port: String = "/dev/ttyS1",
    var baudRate: Int = 9600,
    var stopBits: Int = 1,
    var dataBits: Int = 8,
    var parity: Int = 0,
    var flowCon: Int = 0,
    val flags: Int = 0,
    val isEchoNo: Boolean = true,
    var readHandle: AbsSerialReadHandle = WaitSerialReadHandle(),
    var onReceived: SerialOnDataReceived
) {
    private var serialPort: SerialPort? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var readThread: ReadThread? = null
        private set


    var isOpen = false
        private set


    init {
    }

    fun open(): Boolean {
        isOpen = false
        runCatching {
            serialPort = SerialPort(File(port), baudRate, stopBits, dataBits, parity, flowCon, flags, isEchoNo)
            outputStream = serialPort!!.outputStream
            inputStream = serialPort!!.inputStream
            readThread = ReadThread()
            readThread!!.start()
            isOpen = true
        }.onFailure { it.printStackTrace() }
        return isOpen
    }

    fun close() {
        readThread?.interrupt()
        readThread = null
        serialPort?.close()
        serialPort = null
        isOpen = false
    }

    fun send(byteArray: ByteArray?): Boolean {
        if (byteArray == null || byteArray.isEmpty() || !isOpen) return false
        try {
            outputStream?.write(byteArray)
            return true
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }

    fun sendHex(sHex: String?) {
        send(hexStr2Bytes(sHex))
    }

    fun sendTxt(sTxt: String) {
        send(sTxt.toByteArray())
    }

    /**
     * 十六进制String转换成Byte[]
     */
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

    private inner class ReadThread : Thread() {
        override fun run() {
            super.run()
            while (!isInterrupted) {
                try {
                    if (inputStream == null) {
                        return
                    }
                    val buffer = readHandle.execute(inputStream!!)
                    if (buffer != null && buffer.isNotEmpty()) {
                        onReceived?.invoke(buffer)
                    }
                } catch (e: Throwable) {
                    Log.e("error", e.message!!)
                    return
                }
            }
        }
    }

}