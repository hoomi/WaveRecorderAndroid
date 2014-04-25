package uk.co.o2.android.google.voicerecognizer;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by hoomi on 24/04/2014.
 */
class RecordingThread extends Thread {

    private final static String TAG="RecordingThread";
    private AudioRecord audioRecord;
    private String mFileName = "";
    private int minBufferSize = 0;
    private boolean record = true;
    private final int SAMPLE_RATE = 16000;
    private final short CHANNEL_NUMBER = 1;
    private final short BITS_PER_SAMPLE = 16;

    RecordingThread() {
        minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                CHANNEL_NUMBER == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
                BITS_PER_SAMPLE == 16 ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT);
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/audiorecordtest.wav";
    }

    private void initAudioRecord() {
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                CHANNEL_NUMBER == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
                BITS_PER_SAMPLE == 16 ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT,
                minBufferSize
        );
    }


    public void run() {
        initAudioRecord();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (audioRecord != null && audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            record = true;
            audioRecord.startRecording();
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(mFileName);
                byte[] buffer = new byte[minBufferSize];
                while (audioRecord.read(buffer, 0, minBufferSize) > 0 && record) {
                    fileOutputStream.write(buffer);
                }
                fileOutputStream.flush();
                fileOutputStream.close();
                addWavHeader();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }
            Log.i(TAG, "Stopped Recording");
        } else {
            Log.i(TAG, "Could not initialize AudioRecord");

        }
    }

    private void addWavHeader() throws IOException {
        File file = new File(mFileName);
        if (!file.exists()) {
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(44);
        byteBuffer.putInt(0x52494646);//"RIFF"
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putInt((int) (file.length() + 36));
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putInt(0x57415645);//"WAVE"
        byteBuffer.putInt(0x666D7420); //"fmt"
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putInt(16);
        byteBuffer.putShort((short) 1); //Type is PCM
        byteBuffer.putShort(CHANNEL_NUMBER); //Number of channels
        byteBuffer.putInt(SAMPLE_RATE); //Sampling rate
        byteBuffer.putInt(SAMPLE_RATE * BITS_PER_SAMPLE * CHANNEL_NUMBER / 8); //(Sample Rate * BitsPerSample * Channels) / 8
        byteBuffer.putShort((short) (BITS_PER_SAMPLE * CHANNEL_NUMBER / 8)); //(BitsPerSample * Channels) / 8.1 - 8 bit mono2 - 8 bit stereo/16 bit mono4 - 16 bit stereo
        byteBuffer.putShort(BITS_PER_SAMPLE); //	 Bits per sample
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putInt(0x64617461); //"data" chunk header. Marks the beginning of the data section.
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putInt((int) file.length()); //Size of the data section.
        RandomAccessFile f = new RandomAccessFile(file, "rw");
        f.seek(0); // to the beginning
        f.write(byteBuffer.array());
        f.close();
    }

    public void stopRecording() {
        record = false;
    }

    public String getFileName() {
        return mFileName;
    }
}
