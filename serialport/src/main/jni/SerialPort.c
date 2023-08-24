/*
 * Copyright 2009-2011 Cedric Priscal
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

//#include <termios.h>
#include <termios.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>
#include <jni.h>

#include "SerialPort.h"

#include "android/log.h"

static const char *TAG = "serial_port";
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

/*
 * Class:     android_serialport_SerialPort
 * Method:    open
 * Signature: (Ljava/lang/String;II)Ljava/io/FileDescriptor;
 */
JNIEXPORT jobject JNICALL Java_android_1serialport_1api_SerialPort_open
        (JNIEnv *env, jclass thiz, jstring path, jint baudrate, jint stopBits, jint dataBits,
         jint parity, jint flowCon, jint flags, jboolean isEchoNo) {
    int fd;
    speed_t speed;
    jobject mFileDescriptor;

    /* 检查参数 */
    {
        speed = baudrate;
        if (speed == -1) {
            /* TODO: throw an exception */
            LOGE("Invalid baudrate");
            return NULL;
        }
    }

    /* 打开设备 */
    {
        jboolean iscopy;
        const char *path_utf = (*env)->GetStringUTFChars(env, path, &iscopy);
        LOGD("Opening serial port %s with flags 0x%x", path_utf, O_RDWR | flags);
        fd = open(path_utf, O_RDWR | flags);
        LOGD("open() fd = %d", fd);
        (*env)->ReleaseStringUTFChars(env, path, path_utf);
        if (fd == -1) {
            /* Throw an exception */
            LOGE("Cannot open port");
            /* TODO: throw an exception */
            return NULL;
        }
    }

    /* 配置设备 */
    {
        struct termios2 cfg;
        //读取参数
        ioctl(fd, TCGETS2, &cfg);
        // 设置 波特率
        cfg.c_cflag &= ~CBAUD;
        cfg.c_cflag |= BOTHER;

        cfg.c_ispeed = baudrate;
        cfg.c_ospeed = baudrate;

        cfg.c_cflag &= ~CSIZE;
        //数据位
        switch (dataBits) {
            case 5:
                cfg.c_cflag |= CS5;    //5个数据位
                break;
            case 6:
                cfg.c_cflag |= CS6;    //6个数据位
                break;
            case 7:
                cfg.c_cflag |= CS7;    //7个数据位
                break;
            case 8:
                cfg.c_cflag |= CS8;    //8个数据位
                break;
            default:
                cfg.c_cflag |= CS8;
                break;
        }
        //奇偶校验位
        switch (parity) {
            case 0:
                cfg.c_cflag &= ~PARENB;    //奇偶校验关
                break;
            case 1:
                cfg.c_cflag |= (PARODD | PARENB);   //PARITY ODD
                cfg.c_iflag &= ~IGNPAR;
                cfg.c_iflag |= PARMRK;
                cfg.c_iflag |= INPCK;
                break;
            case 2:
                cfg.c_iflag &= ~(IGNPAR | PARMRK); //PARITY EVEN
                cfg.c_iflag |= INPCK;
                cfg.c_cflag |= PARENB;
                cfg.c_cflag &= ~PARODD;
                break;
            case 3:
                //  PARITY SPACE
                cfg.c_iflag &= ~IGNPAR;             //  确保不会忽略错误的奇偶校验
                cfg.c_iflag |= PARMRK;              // 标记奇偶校验错误，奇偶校验错误
                //  is given as three char sequence
                cfg.c_iflag |= INPCK;               //  启用输入奇偶校验
                cfg.c_cflag |= PARENB | CMSPAR;     // 启用奇偶校验并设置空间奇偶校验
                cfg.c_cflag &= ~PARODD;             //
                break;
            case 4:
                //  PARITY MARK
                cfg.c_iflag &= ~IGNPAR;             // 确保不会忽略错误的奇偶校验
                cfg.c_iflag |= PARMRK;              // 标记奇偶校验错误，奇偶校验错误
                //  is given as three char sequence
                cfg.c_iflag |= INPCK;               //  启用输入奇偶校验
                cfg.c_cflag |= PARENB | CMSPAR | PARODD;
                break;
            default:
                cfg.c_cflag &= ~PARENB;
                break;
        }
        //停止位
        switch (stopBits) {
            case 1:
                cfg.c_cflag &= ~CSTOPB;    //1个停止位
                break;
            case 2:
                cfg.c_cflag |= CSTOPB;    //2个停止位
                break;
            default:
                cfg.c_cflag &= ~CSTOPB;
                break;
        }

        // 硬件流量控制
        switch (flowCon) {
            case 0:
                cfg.c_cflag &= ~CRTSCTS;    //无控制
                break;
            case 1:
                cfg.c_cflag |= CRTSCTS;    //硬件流量控制 RTS/CTS
                break;
            case 2:
                cfg.c_cflag |= IXON | IXOFF | IXANY;    //软件流控制 XON/XOFF
                break;
            default:
                cfg.c_cflag &= ~CRTSCTS;//无控制
                break;
        }
        // 有数据立马可以读取到，不需要换行（0A）
        if (isEchoNo) {
            cfg.c_lflag &= ~(ICANON | ECHO | ECHOE);
        }
        //设置参数
        if (ioctl(fd, TCSETS2, &cfg)) {
            LOGE("tcsets2() failed");
            close(fd);
            /* TODO: throw an exception */
            return NULL;
        }
    }
    /* 创建相应的文件描述符 */
    {
        jclass cFileDescriptor = (*env)->FindClass(env, "java/io/FileDescriptor");
        jmethodID iFileDescriptor = (*env)->GetMethodID(env, cFileDescriptor, "<init>", "()V");
        jfieldID descriptorID = (*env)->GetFieldID(env, cFileDescriptor, "descriptor", "I");
        mFileDescriptor = (*env)->NewObject(env, cFileDescriptor, iFileDescriptor);
        (*env)->
                SetIntField(env, mFileDescriptor, descriptorID, (jint)
                fd);
    }

    return
            mFileDescriptor;
}

/*
 * Class:     cedric_serial_SerialPort
 * Method:    close
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_android_1serialport_1api_SerialPort_close
        (JNIEnv *env, jobject thiz) {
    jclass SerialPortClass = (*env)->GetObjectClass(env, thiz);
    jclass FileDescriptorClass = (*env)->FindClass(env, "java/io/FileDescriptor");

    jfieldID mFdID = (*env)->GetFieldID(env, SerialPortClass, "fd", "Ljava/io/FileDescriptor;");
    jfieldID descriptorID = (*env)->GetFieldID(env, FileDescriptorClass, "descriptor", "I");

    jobject mFd = (*env)->GetObjectField(env, thiz, mFdID);
    jint descriptor = (*env)->GetIntField(env, mFd, descriptorID);

    LOGD("close(fd = %d)", descriptor);
    close(descriptor);
}

