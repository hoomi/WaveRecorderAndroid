package uk.co.o2.android.google.voicerecognizer;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.apache.http.HttpStatus;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class AudioRecordTest extends Activity {
    private static final String LOG_TAG = "AudioRecordTest";
    private static String mFileName = null;

    private RecordButton mRecordButton = null;

    private RecordingThread mRecordingThread = null;

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }


    private void startRecording() {
        mRecordingThread = new RecordingThread();
        mRecordingThread.start();
    }

    private void stopRecording() {
        if (mRecordingThread != null) {
            mRecordingThread.stopRecording();
        }
    }

    class RecordButton extends Button {
        boolean mStartRecording = true;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                onRecord(mStartRecording);
                if (mStartRecording) {
                    setText("Stop recording");
                } else {
                    setText("Start recording");
                }
                mStartRecording = !mStartRecording;
            }
        };

        public RecordButton(Context ctx) {
            super(ctx);
            setText("Start recording");
            setOnClickListener(clicker);
        }
    }


    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        mRecordButton = new RecordButton(this);
        ll.addView(mRecordButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0)
        );
        Button sendButton = new Button(this);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendToGoogleServer();
            }
        });
        sendButton.setText("Send");
        ll.addView(sendButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0)
        );
        setContentView(ll);
    }

    private void sendToGoogleServer() {
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... params) {
                HttpURLConnection urlConnection = null;
                try {
                    URL url = new URL("https://api.nexiwave.com/SpeechIndexing/file/storage//recording/?response=application/json&transcriptFormat=html&auto-redirect=true");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestProperty("Content_Type", "audio/vnd.wav");
                    urlConnection.setDoOutput(true);
                    urlConnection.setDoInput(true);
                    urlConnection.setAllowUserInteraction(false);
                    urlConnection.setConnectTimeout(20 * 60 * 1000);
                    urlConnection.connect();
                    if (urlConnection.getResponseCode() == HttpStatus.SC_OK) {
                        OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
                        writeStream(out);
                    }

                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    String result = readStream(in);
                } catch (IOException ioe) {
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }

                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                if (!TextUtils.isEmpty(s)) {
                    Toast.makeText(AudioRecordTest.this, s, Toast.LENGTH_LONG).show();
                }
            }
        }.execute();

    }

    private void writeStream(OutputStream outputStream) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(mRecordingThread.getFileName());
        try {
            byte[] buffer = new byte[1024];
            while (fileInputStream.read(buffer) > 0) {
                outputStream.write(buffer);
            }
            outputStream.flush();
        } finally {
            fileInputStream.close();
        }
    }

    private String readStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return null;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();

        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line + "\n");
        }
        return sb.toString();

    }

    @Override
    public void onPause() {
        super.onPause();
        stopRecording();
    }
}
