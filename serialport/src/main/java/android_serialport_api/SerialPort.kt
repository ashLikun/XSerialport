/*
 * Copyright 2009 Cedric Priscal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android_serialport_api

import android.util.Log
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class SerialPort(device: File, baudrate: Int, stopBits: Int, dataBits: Int, parity: Int, flowCon: Int, flags: Int, isEchoNo: Boolean) {
    companion object {
        private const val TAG = "SerialPort"

        init {
            System.loadLibrary("serialport")
        }
    }

    /*
     * 不要删除或重命名字段mFd：它由本机方法close（）使用；
     */
    private val fd: FileDescriptor?
    private val fileInputStream: FileInputStream
    private val fileOutputStream: FileOutputStream

    // Getters和setter
    val inputStream: InputStream
        get() = fileInputStream
    val outputStream: OutputStream
        get() = fileOutputStream

    external fun close()

    /**
     * 串口有5个参数：串行设备名、波特率、校验位、数据位、停止位其中校验位一般默认为NONE，数据位一般默认8，停止位默认1
     *
     * @param path     到串行设备的数据对
     * @param baudrate [BAUDRATE] 波特率
     * @param stopBits [STOPB] 停止位
     * @param dataBits [DATAB] 数据位
     * @param parity   [PARITY] 校验位
     * @param flowCon  [FLOWCON] 数据流
     * @param flags    O_RDWR读写模式打开|O_NOCTTY不允许进程管理串行端口|O_NDELAY非阻塞
     * @param isEchoNo   true:有数据立马可以读取到，不需要换行（0A）
     * @return
     */
    external fun open(path: String, baudrate: Int, stopBits: Int, dataBits: Int, parity: Int, flowCon: Int, flags: Int, isEchoNo: Boolean): FileDescriptor?

    init {

        /* 检查访问权限 */
        if (!device.canRead() || !device.canWrite()) {
            try {
                /* 缺少读写权限，正在尝试chmod文件 */
                val su: Process = Runtime.getRuntime().exec("/system/bin/su")
                val cmd = """
                    chmod 666 ${device.absolutePath}
                    exit
                    
                    """.trimIndent()
                su.outputStream.write(cmd.toByteArray())
                if (su.waitFor() != 0 || !device.canRead() || !device.canWrite()) {
                    throw SecurityException()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw SecurityException()
            }
        }
        fd = open(device.absolutePath, baudrate, stopBits, dataBits, parity, flowCon, flags, isEchoNo)
        if (fd == null) {
            Log.e(TAG, "native open returns null")
            throw IOException()
        }
        fileInputStream = FileInputStream(fd)
        fileOutputStream = FileOutputStream(fd)
    }


}