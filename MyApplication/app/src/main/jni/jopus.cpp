#include <jni.h>

#include "opus.h"

#define IMPL_FUNC_DEC_(x,y,z) Java_##y##_##z##_##x

#define FUNC_DEC(x) IMPL_FUNC_DEC_(x, com_eilenthil_opussample , OpusEncoder)

struct OpusEncoderContext {
    int sample_rate;
    int bitrate;
    int channels;
    int mode;
    OpusEncoder* encoder;
};
#include <string>
extern "C"{
    JNIEXPORT jint FUNC_DEC(encoderCreate)(
            JNIEnv * env,
            jobject thiz,
            jlongArray jOutContextArr,
            jint jSampleRate,
            jint jBitrate,
            jint jChannelCount,
            jint jMode){

        jint java_ret = -1;
        int opus_error = 0;
        auto context = new OpusEncoderContext();
        context->sample_rate = jSampleRate;
        context->bitrate = jBitrate;
        context->channels = jChannelCount;
        context->mode = jMode;

        context->encoder = opus_encoder_create(
                context->sample_rate,
                context->channels,
                context->mode,
                &opus_error);

        if (context->encoder != nullptr){
            opus_encoder_ctl(context->encoder, OPUS_SET_BITRATE(context->bitrate));

            jlong * out_ptr = env->GetLongArrayElements(jOutContextArr,nullptr);
            *out_ptr = (jlong)context;
            env->ReleaseLongArrayElements(jOutContextArr,out_ptr,0);
            java_ret = 0;
        } else {
            java_ret = opus_error;
            delete context;
        }
        return java_ret;
    }

    JNIEXPORT jint FUNC_DEC(encoderDestroy)(
            JNIEnv * env,
            jobject thiz,
            jlong jContext){
        if (jContext != 0){
            auto context = (OpusEncoderContext *)jContext;
            delete context;
        }
        return 0;
    }

    JNIEXPORT jint FUNC_DEC(encoderEncode)(
            JNIEnv * env,
            jobject thiz,
            jlong  jContext,
            jbyteArray jInput,
            jint jInputOffset,
            jint jInputLen,
            jbyteArray jOutput,
            jint    jOutputOffset,
            jint    jOutputLen,
            jintArray jOutputWrittenArr){

        jbyte* input = env->GetByteArrayElements(jInput, nullptr);
        jbyte* output = env->GetByteArrayElements(jOutput, nullptr);

        if (jContext != 0) {
            auto context = (OpusEncoderContext *) jContext;
            int written = opus_encode(context->encoder,
                    (opus_int16*)(input + jInputOffset),
                    jInputLen / 2,
                    (unsigned char*)output + jOutputOffset,
                    jOutputLen);

            if (written > 0) {
                jint *out_write = env->GetIntArrayElements(jOutputWrittenArr, nullptr);
                *out_write = written;
                env->ReleaseIntArrayElements(jOutputWrittenArr,out_write,0);
            }
        }

        env->ReleaseByteArrayElements(jInput,input,0);
        env->ReleaseByteArrayElements(jOutput,output,0);
        return 0;
    }
};