package com.eilenthil.opussample;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaDataSource;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FirstFragment extends Fragment {
    // 1 MB should be enough
    private static final int MAX_RAW_RECORD_BUFFER_SIZE = 1024 * 1024;

    Switch permission_switch;
    TextView permission_msg;
    TextView record_data_msg;
    TextView    encode_msg;
    Button  record;
    Button  encode;
    boolean recording;
    byte[]  raw_audio_buffer;
    int     raw_audio_buffer_written;
    Thread  rec_thread;

    Thread enc_thread;
    boolean encoding;

    byte[]  opus_audio_buffer;
    int     opus_audio_buffer_written;
    private final Runnable record_task = new Runnable() {
        private final static String TAG = "RecThread";
        @Override
        public void run() {
            Log.i(TAG, "Starting");
            raw_audio_buffer = new byte[MAX_RAW_RECORD_BUFFER_SIZE];
            raw_audio_buffer_written = 0;

            int last_spam_written = 0;

            int temp_buff_len = AudioRecord.getMinBufferSize(48000,1, AudioFormat.ENCODING_PCM_16BIT);
            byte[] temp_buff = new byte[temp_buff_len];
            AudioRecord rec = new AudioRecord(0,48000,1, AudioFormat.ENCODING_PCM_16BIT, temp_buff_len);
            Log.i(TAG, "Starting record");
            rec.startRecording();
            while (raw_audio_buffer_written < raw_audio_buffer.length){
                int toWrite = raw_audio_buffer.length - raw_audio_buffer_written < temp_buff_len?
                        raw_audio_buffer.length - raw_audio_buffer_written:temp_buff_len;
                int written = rec.read(raw_audio_buffer,raw_audio_buffer_written, toWrite);
                raw_audio_buffer_written += written;

                if (raw_audio_buffer_written - last_spam_written > 10000){
                    Log.i(TAG,"Written " + raw_audio_buffer_written + " / " + MAX_RAW_RECORD_BUFFER_SIZE);
                    last_spam_written = raw_audio_buffer_written;
                }

                FirstFragment.this.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        record_data_msg.setText("Have "+ raw_audio_buffer_written + "/" + MAX_RAW_RECORD_BUFFER_SIZE + " bytes of recorded data");
                    }
                });
            }

            rec.stop();
            rec = null;
            Log.i(TAG, "Done");
        }
    };

    private final Runnable dummy_task = new Runnable() {
        @Override
        public void run() {
            OpusEncoder encoder = OpusEncoder.create();
            int read = 0;

            if (encoder.initialize(48000,128000,1,OpusEncoder.OPUS_APPLICATION_AUDIO)
                    != 0){
                Log.e("OpusBackThread","Failed to init encoder");



            } else {
                Log.i("OpusBackThread","Encoder init");
                opus_audio_buffer = new byte[MAX_RAW_RECORD_BUFFER_SIZE];
                opus_audio_buffer_written = 0;
                int read_offset = 0;
                int write_offset = 0;
                int min_read_chunk_size = 2880* 2;
                while (read_offset < raw_audio_buffer_written){
                    int written = encoder.encode(raw_audio_buffer,read_offset, min_read_chunk_size, opus_audio_buffer, write_offset);

                    read_offset+=min_read_chunk_size;
                    write_offset+=written;
                    final int r_o = read_offset;
                    final int w_o = write_offset;
                    FirstFragment.this.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            encode_msg.setText("Read " + r_o + "/" + raw_audio_buffer_written + " -> " + w_o);
                        }
                    });
                }
                opus_audio_buffer_written = write_offset;
                encoder.terminate();
                Log.i("OpusBackThread","Encoder Terminated");
            }
        }
    } ;

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults){
        if (requestCode == MainActivity.REQUEST_RECORD_AUDIO_PERM_CODE){
            if (    permissions.length > 0 &&
                    permissions[0].compareTo(Manifest.permission.RECORD_AUDIO) == 0){
                permission_switch.setEnabled(true);
                permission_switch.setChecked(grantResults[0] == PackageManager.PERMISSION_GRANTED);
            }
        }
    }


    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false);
    }



    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        permission_switch =view.findViewById(R.id.permission_ok_first);
        permission_msg = view.findViewById(R.id.permission_msg_first);
        record = view.findViewById(R.id.record_first);
        record_data_msg = view.findViewById(R.id.recorded_raw_size_text_first);
        encode_msg = view.findViewById(R.id.encode_text_button_first);
        encode = view.findViewById(R.id.encode_raw_button_first);


        boolean have_record_audio = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED;
        permission_msg.setVisibility(have_record_audio?View.VISIBLE:View.INVISIBLE);
        permission_switch.setChecked(have_record_audio);
        permission_switch.setEnabled(!have_record_audio);
        recording = false;
        encoding = false;

        permission_switch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                permission_switch.setEnabled(false);
                if (permission_switch.isChecked()){
                    String [] perm = new String[]{ Manifest.permission.RECORD_AUDIO};
                    ActivityCompat.requestPermissions(getActivity(), perm, MainActivity.REQUEST_RECORD_AUDIO_PERM_CODE);
                } else {
                    permission_msg.setVisibility(View.VISIBLE);
                }
            }
        });

        view.findViewById(R.id.button_first).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        });

        record.setVisibility(have_record_audio?View.VISIBLE:View.GONE);
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recording = ! recording;
                if (recording){
                    record.setText("Stop");
                    rec_thread = new Thread(record_task);
                    rec_thread.start();
                } else {
                    record.setText("Record");
                    if (rec_thread != null){
                        try {
                            rec_thread.join();
                        } catch (InterruptedException e) {

                        }
                    }
                    rec_thread = null;
                }
            }
        });

        encode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                encoding = !encoding;
                if (encoding){
                    enc_thread = new Thread(dummy_task);
                    enc_thread.start();
                    encode.setText("Stop");
                } else {
                    encode.setText("Encode");
                    try {
                        enc_thread.join();
                    } catch (InterruptedException e) {

                    }
                    enc_thread = null;
                }
            }
        });
    }
}