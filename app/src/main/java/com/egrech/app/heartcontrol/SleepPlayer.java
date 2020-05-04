package com.egrech.app.heartcontrol;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import polar.com.sdk.api.PolarBleApi;
import polar.com.sdk.api.PolarBleApiCallback;
import polar.com.sdk.api.PolarBleApiDefaultImpl;
import polar.com.sdk.api.errors.PolarInvalidArgument;
import polar.com.sdk.api.model.PolarDeviceInfo;
import polar.com.sdk.api.model.PolarHrBroadcastData;
import polar.com.sdk.api.model.PolarHrData;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;

public class SleepPlayer extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 356;
    private static final int SET_UP_NEW_USER = 14;
    private static final int RESULT_CANCELED = 357;
    private static final int REQUEST_ENABLE_GPS = 3358;

    String DEVICE_ID = "612F262A"; // or bt address like F5:A7:B8:EF:7A:D1 // TODO replace with your device id

    PolarBleApi api;
    Disposable broadcastDisposable;

    Boolean isBluetoothOn = false;
    Boolean isGPSOn = false;

    private ImageView playBackButton;
    private ImageView playNextButton;

    private ProgressBar progressBar;

    int averageHeartRat = 90; // todo doriesit analyzator priemerneho tepu

    int heartRate;
    int senzorData = 0;

    private ImageView mode_button;

    private ImageView heartRateBackground;
    private TextView heartRateinfo;

    boolean mode_menu_open = false;

    public static final String TAG = "SleepPlayerActivity";

    private SeekBar mSeekbarAudio;
    private PlayerAdapter mPlayerAdapter;
    private boolean mUserIsSeeking = false;

    private TextView player_song_title;
    private TextView player_song_artist;
    private TextView player_actual_time;
    private TextView player_song_duration;


    private ArrayList<Song> songList;

    int songPosition = 0;

    private ArrayList<Integer> playedSongs;


    private NumberPicker hrPicker;

    private CountDownTimer countDown;
    private long timeToCount = 10000;
    boolean timeRunning;

    private TextView timeCount;
    private boolean writeFlag;
//    private Button startCountdown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep_player);
        checkForPermission();
        //enable bluetooth
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        //enable GPS
//        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
//        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//            Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//            startActivityForResult(gpsOptionsIntent, REQUEST_ENABLE_GPS);
//        }
        heartRateBackground = (ImageView) findViewById(R.id.sport_player_heart_rate_background);
        heartRateinfo = (TextView) findViewById(R.id.sleep_palyer_heart_rate);
        // inicializuj pripojenie OH1 senzoru
        connectPolarDevice();
        songList = new ArrayList<Song>();
        getSongList();

        writeFlag = false;

        initializeUI();
        initializeSeekbar();
        initializePlaybackController();
    }

    void checkForPermission() {
        // check for storage permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted so we ask for it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    12);
        }

        // check for location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted so we ask for it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    12);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted so we ask for it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    12);
        }
    }

    public void getSongList() {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
//        String selection = MediaStore.Audio.Media.ALBUM + " == 'Emotions'";
        String selection = MediaStore.Audio.Media.IS_MUSIC;

        Cursor musicCursor = musicResolver.query(musicUri, null, selection, null, null);

        if (musicCursor != null && musicCursor.moveToFirst()) {

            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ARTIST);


            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist, null));
            }
            while (musicCursor.moveToNext());
        }

        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();

        mPlayerAdapter.loadMedia(ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                songList.get(0).getID()), songList.get(0));

        playedSongs.add(songPosition);

        progressBar.setVisibility(View.GONE);
//                    player_actual_time.setVisibility(View.VISIBLE);
//                    player_song_duration.setVisibility(View.VISIBLE);

        player_song_title.setText(songList.get(0).getTitle());
        player_song_artist.setText(songList.get(0).getArtist());

    }


    @Override
    protected void onStop() {
        super.onStop();
        if (isChangingConfigurations() && mPlayerAdapter.isPlaying()) {
            Log.d(TAG, "onStop: don't release MediaPlayer as screen is rotating & playing");
        } else {
            mPlayerAdapter.release();
            Log.d(TAG, "onStop: release MediaPlayer");
        }
    }

    void playNextSong(boolean lastFinished) {

        songPosition += 1;

        player_song_title.setText(songList.get(songPosition).getTitle());
        player_song_artist.setText(songList.get(songPosition).getArtist());

        mPlayerAdapter.loadMedia(ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                songList.get(songPosition).getID()), songList.get(songPosition));

        if (mPlayerAdapter.isPlaying() || lastFinished) {
            mPlayerAdapter.reset();
            mPlayerAdapter.play();
        } else mPlayerAdapter.reset();


        playedSongs.add(songPosition);

    }

    void playBackSong() {
        if (!(playedSongs.size() == 1)) {
            playedSongs.remove(playedSongs.size() - 1);
            songPosition = playedSongs.get(playedSongs.size() - 1);
            player_song_title.setText(songList.get(songPosition).getTitle());
            player_song_artist.setText(songList.get(songPosition).getArtist());
        } else
            Toast.makeText(getApplicationContext(), "Nothing to play before this", Toast.LENGTH_SHORT);

        mPlayerAdapter.loadMedia(ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                songList.get(songPosition).getID()), songList.get(songPosition));
        if (mPlayerAdapter.isPlaying()) {
            mPlayerAdapter.reset();
            mPlayerAdapter.play();
        } else mPlayerAdapter.reset();

    }


    private void initializeUI() {


        timeCount = (TextView) findViewById(R.id.sleep_time);


        progressBar = (ProgressBar) findViewById(R.id.sleep_progressBar);
        playedSongs = new ArrayList<Integer>();


        player_song_title = (TextView) findViewById(R.id.sleep_song_title);
        player_song_artist = (TextView) findViewById(R.id.sleep_song_artist);


//        player_actual_time = (TextView) findViewById(R.id.car_player_actual_time);
//        player_song_duration = (TextView) findViewById(R.id.car_player_song_duration);


//        player_next_layout = (ConstraintLayout) findViewById(R.id.car_player_next_layout);
//        player_back_layout = (ConstraintLayout) findViewById(R.id.car_player_back_layout);
//        player_song_layout = (ConstraintLayout) findViewById(R.id.car_player_song_layout);

//
        mode_button = (ImageView) findViewById(R.id.sleep_mode);

        playBackButton = (ImageView) findViewById(R.id.sleep_play_back);
        playNextButton = (ImageView) findViewById(R.id.sleep_play_next);


        ImageView mPlayButton = (ImageView) findViewById(R.id.sleep_play);

//        mPauseButton = (Button) findViewById(R.id.car_player_button2);
//        mResetButton = (Button) findViewById(R.id.car_player_button3);

        mSeekbarAudio = (SeekBar) findViewById(R.id.sleep_seek_bar);

        Button startTimer = (Button) findViewById(R.id.sleep_button1);

        startTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!timeRunning) {
                    countDown = new CountDownTimer(timeToCount, 1000) {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void onTick(long millisUntilFinished) {
                            timeToCount = millisUntilFinished;
                            int minutes = (int) timeToCount / 10000;
                            int secondes = (int) timeToCount % 10000 / 1000;

                            if (secondes < 10) {
                                timeCount.setText(minutes + ":0" + secondes);
                            }
                            timeCount.setText(minutes + ":" + secondes);
                            timeRunning = true;
                        }

                        @Override
                        public void onFinish() {
//                            writeTimeStamp();
                        }
                    }.start();
                } else {
                    countDown.cancel();
                    timeRunning = false;

                }
            }
        });


        mode_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mode_menu_open) {
                    mode_menu_open = false;
                } else {
                    mode_menu_open = true;
                }
            }
        });

        playNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNextSong(false);
                playNextButton.setImageResource(R.drawable.forward_button);

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        playNextButton.setImageResource(R.drawable.sleep_forward_button);
                    }
                }, 200);
            }
        });
        playBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playBackSong();
                playBackButton.setImageResource(R.drawable.back_button);

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        playBackButton.setImageResource(R.drawable.sleep_back_button);
                    }
                }, 200);
            }
        });

        mPlayButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mPlayerAdapter.isPlaying()) {
                            mPlayerAdapter.pause();
                            mPlayButton.setImageResource(R.drawable.sleep_play_button);
                        } else {
                            mPlayerAdapter.play();
                            mPlayButton.setImageResource(R.drawable.play_stop_button);
                        }
                    }
                });
    }


    private void writeTimeStamp() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference dbUsersRef = database.getReference("sleeper");

        String time = String.valueOf(Calendar.getInstance().getTime());
        timeCount.setText(time);

        dbUsersRef.child(String.valueOf("sleepSession")).setValue(time).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                mPlayerAdapter.release();
//                Intent intent = new Intent(getApplicationContext(), Menu.class);
//                intent.putExtra("TERMINATE_APP", 1);
//                startActivity(intent);
            }
        });
    }


    private void initializePlaybackController() {
        MediaPlayerHolder mMediaPlayerHolder = new MediaPlayerHolder(this);
        Log.d(TAG, "initializePlaybackController: created MediaPlayerHolder");
        mMediaPlayerHolder.setPlaybackInfoListener(new SleepPlayer.PlaybackListener());
        mPlayerAdapter = mMediaPlayerHolder;
        Log.d(TAG, "initializePlaybackController: MediaPlayerHolder progress callback set");
    }

    private void initializeSeekbar() {
        mSeekbarAudio.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    int userSelectedPosition = 0;

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        mUserIsSeeking = true;
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            userSelectedPosition = progress;
//                            car_player_actual_time.setText(String.valueOf(progress));
                        }
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        mUserIsSeeking = false;
                        mPlayerAdapter.seekTo(userSelectedPosition);
                    }
                });
    }

    public class PlaybackListener extends PlaybackInfoListener {

        @Override
        public void onDurationChanged(int duration) {
            mSeekbarAudio.setMax(duration);
            Log.d(TAG, String.format("setPlaybackDuration: setMax(%d)", duration));
        }

        @Override
        public void onPositionChanged(int position) {
            if (!mUserIsSeeking) {
                mSeekbarAudio.setProgress(position, true);
                Log.d(TAG, String.format("setPlaybackPosition: setProgress(%d)", position));
            }
        }

        @Override
        public void onStateChanged(@State int state) {
            String stateToString = PlaybackInfoListener.convertStateToString(state);
            onLogUpdated(String.format("onStateChanged(%s)", stateToString));
        }

        @Override
        public void onPlaybackCompleted() {
            playNextSong(true);
        }


        @Override
        public void onLogUpdated(String message) {
//            if (mTextDebug != null) {
//                mTextDebug.append(message);
//                mTextDebug.append("\n");
//                // Moves the scrollContainer focus to the end.
//                mScrollContainer.post(
//                        new Runnable() {
//                            @Override
//                            public void run() {
//                                mScrollContainer.fullScroll(ScrollView.FOCUS_DOWN);
//                            }
//                        });
//            }
        }
    }


    // tento blok kodu kazdych 7 sekund spravi priemer zo ziskanych hodnot zo senzoru a stanovi priemernu hodnotu tepu
    ArrayList<Integer> hrValues = new ArrayList<Integer>();
    public static Handler myHandler = new Handler();
    private static final int TIME_TO_WAIT = 7000;

    Runnable myRunnable = new Runnable() {
        @Override
        public void run() {
            int hrCount = 0;
            for (int i = 0; i < hrValues.size(); i++) {
                hrCount += hrValues.get(i);
            }

            heartRate = hrCount / hrValues.size();

            //Log.e("HEARTRATE", "Actual avaregae measurment of Heart Rate is: "+heartRate);
            hrValues.clear();
            restartCountingHR();
        }
    };
    // fin

    public void startCountingHR(int hrData) {
        myHandler.postDelayed(myRunnable, TIME_TO_WAIT);
    }

    public void stopCountingHR() {
        myHandler.removeCallbacks(myRunnable);
    }

    public void restartCountingHR() {
        myHandler.removeCallbacks(myRunnable);
        myHandler.postDelayed(myRunnable, TIME_TO_WAIT);
    }


    void connectPolarDevice() {
        final Handler handler = new Handler();
        final boolean[] runnableFlag = {false};

        //polar device
        api = PolarBleApiDefaultImpl.defaultImplementation(this, PolarBleApi.ALL_FEATURES);
        api.setPolarFilter(false);

        api.setApiLogger(new PolarBleApi.PolarBleApiLogger() {
            @Override
            public void message(String s) {
                Log.d(TAG, s);
            }
        });
        Log.d(TAG, "version: " + PolarBleApiDefaultImpl.versionInfo());


        api.setApiCallback(new PolarBleApiCallback() {
            @Override
            public void blePowerStateChanged(boolean powered) {
                Log.d(TAG, "BLE power: " + powered);
            }

            @Override
            public void deviceConnected(PolarDeviceInfo polarDeviceInfo) {
                Log.d(TAG, "CONNECTED: " + polarDeviceInfo.deviceId);
                DEVICE_ID = polarDeviceInfo.deviceId;
            }

            @Override
            public void deviceConnecting(PolarDeviceInfo polarDeviceInfo) {
                Log.d(TAG, "CONNECTING: " + polarDeviceInfo.deviceId);
                DEVICE_ID = polarDeviceInfo.deviceId;
            }

            @Override
            public void deviceDisconnected(PolarDeviceInfo polarDeviceInfo) {
                Log.d(TAG, "DISCONNECTED: " + polarDeviceInfo.deviceId);
            }

            @Override
            public void accelerometerFeatureReady(String identifier) {
                Log.d(TAG, "ACC READY: " + identifier);
                // acc streaming can be started now if needed
            }


            @Override
            public void hrFeatureReady(String identifier) {
                Log.d(TAG, "HR READY: " + identifier);
                // hr notifications are about to start
            }

            @Override
            public void disInformationReceived(String identifier, UUID uuid, String value) {
                Log.d(TAG, "uuid: " + uuid + " value: " + value);

            }

            @Override
            public void batteryLevelReceived(String identifier, int level) {
                Log.d(TAG, "BATTERY LEVEL: " + level);

            }

            @Override
            public void hrNotificationReceived(String identifier, PolarHrData data) {
                Log.d(TAG, "HR value: " + data.hr + " rrsMs: " + data.rrsMs + " rr: " + data.rrs + " contact: " + data.contactStatus + "," + data.contactStatusSupported);
            }

            @Override
            public void polarFtpFeatureReady(String s) {
                Log.d(TAG, "FTP ready");
            }
        });

        try {
            api.connectToDevice(DEVICE_ID);
        } catch (PolarInvalidArgument polarInvalidArgument) {
            polarInvalidArgument.printStackTrace();
        }
        broadcastDisposable = api.startListenForPolarHrBroadcasts(null).subscribe(
                new Consumer<PolarHrBroadcastData>() {
                    @Override
                    public void accept(PolarHrBroadcastData polarBroadcastData) throws Exception {

                        player_song_title.setVisibility(View.VISIBLE);

                        senzorData = polarBroadcastData.hr;

                        heartRateinfo.setText(String.valueOf(senzorData));

                        hrValues.add(senzorData);

                        if(senzorData < 65 && !writeFlag) {
                            writeFlag = true;
                            writeTimeStamp();
                        }


                        if (!runnableFlag[0]) startCountingHR(polarBroadcastData.hr);
                        runnableFlag[0] = true; // when we started average counting, we can disable start of runnable counter

//                        Log.d(TAG, "HR SenZOR OH1 BROADCAST " +
//                                polarBroadcastData.polarDeviceInfo.deviceId + " HR: " +
//                                polarBroadcastData.hr + " batt: " +
//                                polarBroadcastData.batteryStatus);
                    }
                },
                new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e(TAG, "" + throwable.getLocalizedMessage());
                    }
                },
                new Action() {
                    @Override
                    public void run() throws Exception {
                        Log.d(TAG, "complete");
                    }
                }
        );
    }
}
