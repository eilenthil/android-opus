package com.eilenthil.opussample;

public class OpusEncoder {

    /** Best for most VoIP/videoconference applications where listening quality and intelligibility matter most */
    public static final int  OPUS_APPLICATION_VOIP  =   2048;
    /**  Best for broadcast/high-fidelity application where the decoded audio should be as close as possible to the input */
    public static final int OPUS_APPLICATION_AUDIO  =   2049;
    /** Only use when lowest-achievable latency is what matters most. Voice-optimized modes cannot be used. */
    public static final int  OPUS_APPLICATION_RESTRICTED_LOWDELAY = 2051;

    private static final int ALLOWED_SAMPLE_RATES[] = new int[]{8000, 12000, 16000, 24000, 48000};

    /** Native methods **/
    private static native int encoderCreate(
            long [] out,
            int sample_rate,
            int bitrate,
            int channel_count,
            int mode);
    private static native int encoderDestroy(long context);
    private static native int encoderEncode(
            long context,
            byte[] input,
            int input_offset,
            int input_len,
            byte[] out,
            int out_offset,
            int out_len,
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

    public static OpusEncoder create(){
        if (NATIVE_OK){
            return new OpusEncoder();
        }
        return null;
    }

    long handle;
    // This stores the encode call value of written encoded bytes upon return
    int[] written_ref;

    private OpusEncoder() {
        written_ref = new int[1];
        handle = 0;
    }

    private static boolean input_valid(int sample_rate, int mode, int channels){
        boolean r = (   mode == OPUS_APPLICATION_AUDIO ||
                        mode == OPUS_APPLICATION_RESTRICTED_LOWDELAY ||
                        mode == OPUS_APPLICATION_VOIP);
        if (r){
            boolean ok = false;
            for (int i = 0; !ok && i < ALLOWED_SAMPLE_RATES.length; i++){
                ok |= sample_rate == ALLOWED_SAMPLE_RATES[i];
            }
            r = ok && (channels == 1 || channels == 2);
        }
        return r;
    }
    /**
     * initializes a new OpusEncoder
     * @param sample_rate [8000, 12000, 16000, 24000, or 48000]
     * @param bitrate see opus lib for valid bit rate
     * @param channels 1 or 2 [see opus lib]
     * @return 0 - ok
     *      -1 - failed to load jopus.so
     *      -2 - an input argument is invalid
     */
    public int initialize(int sample_rate, int bitrate, int channels, int mode){
        if (!NATIVE_OK){
            return -1;
        }

        if (!input_valid (sample_rate,mode,channels)){
            return -2;
        }
        long [] out_context = new long[1];
        int r = encoderCreate(out_context, sample_rate,bitrate,channels,mode);
        if (r == 0) {
            handle = out_context[0];
        }
        return r;
    }

    /**
     * terminates an OpusEncoder
     * @return
     */
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

    /**
     * encodes the payload of input, using OpusEncoder
     * @param input
     * @param output
     * @return
     */
    public int encode(byte[] input, int in_offset, int in_max_read,byte output[], int out_offset){
        int r = -1;
        if (NATIVE_OK ){
            if (handle == 0) {
                r = -2;
            }else {
                r = encoderEncode(handle,input, in_offset, in_max_read, output,out_offset , output.length, written_ref);
                if (r == 0){
                    r = written_ref[0];
                }
            }

        }
        return r;
    }
}
