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
import java.io.FileReader
import java.io.IOException
import java.io.LineNumberReader
import java.util.Vector

/**
 * @author　　: 李坤
 * 创建时间: 2023/8/22 11:11
 * 邮箱　　：496546144@qq.com
 *
 * 功能介绍：查找设备
 */

class SerialPortFinder {
    companion object {
        private const val TAG = "SerialPort"
    }

    inner class Driver(val name: String, private val deviceRoot: String) {
        val devices by lazy {
            val result = mutableListOf<File>()
            runCatching {
                File("/dev").listFiles().forEach {
                    if (it.absolutePath.startsWith(deviceRoot)) {
                        Log.d(TAG, "Found new device: $it")
                        result.add(it)
                    }
                }
            }
            result
        }
    }

    /**
     * 设备列表
     */
    private val drivers by lazy {
        val result = mutableListOf<Driver>()
        runCatching {
            val r = LineNumberReader(FileReader("/proc/tty/drivers"))
            var l: String
            while (r.readLine().also { l = it } != null) {
                // 由于驱动程序名称可能包含空格，因此我们不使用split（）提取驱动程序名称
                val drivername = l.substring(0, 0x15).trim { it <= ' ' }
                val w = l.split(" +".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (w.size >= 5 && w[w.size - 1] == "serial") {
                    Log.d(TAG, "Found new driver " + drivername + " on " + w[w.size - 4])
                    result.add(Driver(drivername, w[w.size - 4]))
                }
            }
            r.close()
        }
        result
    }


    /**
     * 分析每个驱动程序
     */
    val allDevices: List<String>
        get() {
            val result = mutableListOf<String>()
            drivers.forEach {
                it.devices.forEach { it2 ->
                    val device = it2.name
                    val value = String.format("%s (%s)", device, it.name)
                    result.add(value)
                }
            }
            return result
        }

    /**
     * 分析每个驱动程序
     */
    val allDevicesPath: List<String>
        get() {
            val result = mutableListOf<String>()
            drivers.forEach {
                it.devices.forEach { it2 ->
                    result.add(it2.absolutePath)
                }
            }
            return result
        }

}