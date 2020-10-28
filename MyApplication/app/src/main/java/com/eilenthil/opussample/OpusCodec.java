package com.eilenthil.opussample;

public class OpusCodec {

    /** Native methods **/
    private static native int encoderCreate(
            long [] out,
            int sample_rate,
            int bitrate,
            int channel_count);
    private static native int encoderDestroy(long context);
    private static native int encoderEncode(
            long context,
            byte[] input,
            int input_len,
            byte[] out,
            int[] written);

    /** Did we manage to load the jopus.so **/
    private static final boolean NATIVE_OK;
    static {
        boolean ok = false;
        try {
            System.loadLibrary("jopus");
            ok = true;
        } catch (Exception e){

        }
        NATIVE_OK = ok;
    }

    public static OpusCodec create(){
        if (NATIVE_OK){
            return new OpusCodec();
        }
        return null;
    }

    long handle;

    private OpusCodec() {
        handle = 0;
    }

    public int initialize(int sample_rate, int bitrate, int channels){
        if (!NATIVE_OK){
            return -1;
        }
        long [] out_context = new long[1];
        int r =encoderCreate(out_context, sample_rate,bitrate,channels);
        if (r == 0) {
            handle = out_context[0];
        }
        return r;
    }

    public int terminate() {
        if (!NATIVE_OK){
            return -1;
        }
        int r = -2;
        if (handle != 0){
            r = encoderDestroy(handle);
            handle = 0;
        }

        return r;
    }

}
