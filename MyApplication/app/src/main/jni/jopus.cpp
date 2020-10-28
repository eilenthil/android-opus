#include <jni.h>

#include "opus.h"

#define PACKAGE_PREFIX com_eilenthil_opussample
#define CLASS_NAME      OpusCodec

#define FUN_START Java_PACKAGE_PREFIX_CLASS_NAME_

extern "C"{
    JNIEXPORT jint FUNC_DEC_encoderCreate(
            JNIEnv * env,
            jobject thiz){
        return 0;
    }

};