package com.example.audiorecorder;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.os.Bundle;

import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.cleveroad.audiovisualization.DbmHandler;
import com.cleveroad.audiovisualization.GLAudioVisualizationView;
import com.cleveroad.audiovisualization.SpeechRecognizerDbmHandler;
import com.cleveroad.audiovisualization.VisualizerDbmHandler;

import java.io.File;
import java.io.IOException;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {
    private ImageButton startbtn, stopbtn, playbtn, stopplay;
    private GLAudioVisualizationView audioVisualization;
    private Chronometer chronometer;
    private TextView textView;
    private VisualizerHandler visualizerHandler;

    private MediaRecorder mRecorder;
    private MediaPlayer mPlayer;
    private static final String LOG_TAG = "AudioRecording";
    private File mAudioFile;
    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MainActivity.this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        startbtn = findViewById(R.id.btnRecord);
        stopbtn = findViewById(R.id.btnStop);
        playbtn = findViewById(R.id.btnPlay);
        stopplay = findViewById(R.id.btnStopPlay);
        chronometer = findViewById(R.id.chronometer);
        textView = findViewById(R.id.text);
        textView.setText(R.string.waiting);
        stopbtn.setEnabled(false);
        playbtn.setEnabled(false);
        stopplay.setEnabled(false);
        audioVisualization = findViewById(R.id.visualizer_view);
        try {
            mAudioFile = createAudioFile(this, "demo");
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.e(LOG_TAG, mAudioFile.getAbsolutePath());
        startbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(CheckPermissions()) {
                    stopbtn.setEnabled(true);
                    stopbtn.setVisibility(View.VISIBLE);
                    startbtn.setEnabled(false);
                    startbtn.setVisibility(View.GONE);
                    playbtn.setEnabled(false);
                    playbtn.setVisibility(View.GONE);
                    stopplay.setEnabled(false);
                    stopplay.setVisibility(View.GONE);
                    //设置开始计时时间
                    chronometer.setBase(SystemClock.elapsedRealtime() );
                    textView.setText(R.string.recording);
                    int hour = (int) ((SystemClock.elapsedRealtime() - chronometer.getBase()) / 1000 / 60);
                    chronometer.setFormat("0"+String.valueOf(hour)+":%s");
                    //启动计时器
                    chronometer.start();


                    mRecorder = new MediaRecorder();
                    mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                    mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                    mRecorder.setOutputFile(mAudioFile.getAbsolutePath());
                    AudioManager audioManager = (AudioManager) (MainActivity.this).getSystemService(Context.AUDIO_SERVICE);
                    SpeechRecognizerDbmHandler speechRecHandler = DbmHandler.Factory.newSpeechRecognizerHandler(MainActivity.this);
                    speechRecHandler.innerRecognitionListener();
                    audioVisualization.linkTo(speechRecHandler);
                    try {
                        mRecorder.prepare();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "prepare() failed");
                    }
                    mRecorder.start();
                    // Toast.makeText(getApplicationContext(), "Recording Started", Toast.LENGTH_LONG).show();
                }
                else
                {
                    RequestPermissions();
                }
            }
        });
        stopbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopbtn.setEnabled(false);
                stopbtn.setVisibility(View.GONE);
                startbtn.setEnabled(true);
                startbtn.setVisibility(View.VISIBLE);
                playbtn.setEnabled(true);
                playbtn.setVisibility(View.VISIBLE);
                stopplay.setEnabled(true);
                stopplay.setVisibility(View.VISIBLE);
                chronometer.stop();
                textView.setText(R.string.rfinished);
                mRecorder.release();
                mRecorder = null;
                // Toast.makeText(getApplicationContext(), "Recording Stopped", Toast.LENGTH_LONG).show();
            }
        });
        playbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopbtn.setEnabled(false);
                stopbtn.setVisibility(View.GONE);
                startbtn.setEnabled(true);
                startbtn.setVisibility(View.VISIBLE);
                playbtn.setEnabled(false);
                playbtn.setVisibility(View.GONE);
                stopplay.setEnabled(true);
                stopplay.setVisibility(View.VISIBLE);
                textView.setText(R.string.playing);
                //设置开始计时时间
                chronometer.setBase(SystemClock.elapsedRealtime() );
                int hour = (int) ((SystemClock.elapsedRealtime() - chronometer.getBase()) / 1000 / 60);
                chronometer.setFormat("0"+String.valueOf(hour)+":%s");
                //启动计时器
                chronometer.start();
                mPlayer = new MediaPlayer();
                System.out.println("------->" + mPlayer.getAudioSessionId());
                try {
                    mPlayer.setDataSource(mAudioFile.getAbsolutePath());
                    mPlayer.prepare();
                    mPlayer.start();
                    // Toast.makeText(getApplicationContext(), "Recording Started Playing", Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "prepare() failed");
                }
                mPlayer.setLooping(false);
                audioVisualization.linkTo(DbmHandler.Factory.newVisualizerHandler(MainActivity.this, mPlayer));
                audioVisualization.post(new Runnable() {
                    @Override
                    public void run() {
                        mPlayer.setOnCompletionListener(v -> {
                            audioVisualization.release();
                            if(visualizerHandler != null) {
                                //visualizerHandler.stop();
                            }

                            if(mPlayer != null){
                                try {
                                    mPlayer.stop();
                                    mPlayer.reset();
                                    chronometer.stop();
                                    audioVisualization.release();
                                    textView.setText(R.string.pfinished);
                                } catch (Exception e){ }
                            }

                        });
                    }
                });

            }
        });
        stopplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayer.release();
                mPlayer = null;
                stopbtn.setEnabled(false);
                stopbtn.setVisibility(View.GONE);
                startbtn.setEnabled(true);
                startbtn.setVisibility(View.VISIBLE);
                playbtn.setEnabled(true);
                playbtn.setVisibility(View.VISIBLE);
                stopplay.setEnabled(false);
                stopplay.setVisibility(View.GONE);
                chronometer.stop();
                textView.setText(R.string.paused);
                // Toast.makeText(getApplicationContext(),"Playing Audio Stopped", Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_AUDIO_PERMISSION_CODE:
                if (grantResults.length> 0) {
                    boolean permissionToRecord = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean permissionToStore = grantResults[1] ==  PackageManager.PERMISSION_GRANTED;
                    if (permissionToRecord && permissionToStore) {
                        Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(),"Permission Denied",Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }
    public boolean CheckPermissions() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }
    private void RequestPermissions() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, WRITE_EXTERNAL_STORAGE}, REQUEST_AUDIO_PERMISSION_CODE);
    }
    private static File createAudioFile(Context context, String audioName) throws IOException {
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PODCASTS);
        File audio = File.createTempFile(
                audioName,  /* prefix */
                ".3gp",         /* suffix */
                storageDir      /* directory */
        );

        return audio;
    }
    @Override
    public void onResume() {
        super.onResume();
        audioVisualization.onResume();
    }

    @Override
    public void onPause() {
        audioVisualization.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        audioVisualization.release();
        super.onDestroy();
    }


}
