package com.egrech.app.heartcontrol;

import androidx.appcompat.app.AppCompatActivity;
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
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;

public class CarPlayer extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 356;
    private static final int SET_UP_NEW_USER = 14;
    private static final int RESULT_CANCELED = 357;
    private static final int REQUEST_ENABLE_GPS = 3358;

    String DEVICE_ID = "612F262A"; // or bt address like F5:A7:B8:EF:7A:D1 // TODO replace with your device id

    PolarBleApi api;
    Disposable broadcastDisposable;

    Boolean isBluetoothOn = false;
    Boolean isGPSOn = false;


    int averageHeartRat = 90; // todo doriesit analyzator priemerneho tepu
    int heartRate = averageHeartRat;

    Button auto_primary;
    Button sleep_secondary;
    Button sport_secondary;
    Button player_secondary;

    Button playNext_button;
    Button PlayBackButton;

    ImageView heartRateBackground;
    TextView heartRateinfo;

    int lastSongPosition = 0;

    boolean mode_menu_open = false;

    public static final String TAG = "MainActivity";
    public static final int MEDIA_RES_ID = R.raw.jazz_in_paris;

    private TextView mTextDebug;
    private SeekBar mSeekbarAudio;
    private ScrollView mScrollContainer;
    private PlayerAdapter mPlayerAdapter;
    private boolean mUserIsSeeking = false;


    private ArrayList<Song> songList;

    Song currentSong;
    int songPosition = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_player);

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

        heartRateBackground = (ImageView) findViewById(R.id.car_player_heart_rate_background);
        heartRateinfo = (TextView) findViewById(R.id.car_palyer_heart_rate);

        // inicializuj pripojenie OH1 senzoru
        connectPolarDevice();


        songList = new ArrayList<Song>();

        // ziskaj list piesni v zariadeni a utried ich abecedne
        getSongList();
        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
        // ziskaj list piesni v zariadeni a utried ich abecedne FINN


        initializeUI();
        initializeSeekbar();
        initializePlaybackController();
        Log.d(TAG, "onCreate: finished");
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
                String emotion = getEmotionStatusFromFile((int) thisId);
                if (emotion.equals("")) emotion = "X";
                songList.add(new Song(thisId, thisTitle, thisArtist, emotion));
            }
            while (musicCursor.moveToNext());
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        mPlayerAdapter.loadMedia(ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                songList.get(0).getID()), songList.get(0));
        Log.d(TAG, "onStart: create MediaPlayer");
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

    void playNextSong() {
        songPosition = searchSongByEmotion();
        mPlayerAdapter.loadMedia(ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                songList.get(songPosition).getID()), songList.get(songPosition));

        mPlayerAdapter.reset();
        mPlayerAdapter.play();
    }

    void playBackSong() {
        songPosition = lastSongPosition;
        mPlayerAdapter.loadMedia(ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                songList.get(songPosition).getID()), songList.get(songPosition));
        mPlayerAdapter.reset();
        mPlayerAdapter.play();
    }


    private void initializeUI() {

        Animation animSlideIn = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.fui_slide_in_right);

        auto_primary = (Button) findViewById(R.id.mode_car);
        sleep_secondary = (Button) findViewById(R.id.mode_sleep);
        sport_secondary = (Button) findViewById(R.id.mode_sport);
        player_secondary = (Button) findViewById(R.id.mode_normal);

        sleep_secondary.setAnimation(animSlideIn);
        sport_secondary.setAnimation(animSlideIn);
        player_secondary.setAnimation(animSlideIn);

        auto_primary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                 if(mode_menu_open) {
                     mode_menu_open = false;
                     sleep_secondary.setVisibility(View.GONE);
                     sport_secondary.setVisibility(View.GONE);
                     player_secondary.setVisibility(View.GONE);
                 } else {
                     mode_menu_open = true;
                     sleep_secondary.setVisibility(View.VISIBLE);
                     sport_secondary.setVisibility(View.VISIBLE);
                     player_secondary.setVisibility(View.VISIBLE);
                 }
            }
        });

        sleep_secondary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SleepPlayer.class);
                startActivity(intent);
            }
        });

        sport_secondary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SportPlayer.class);
                startActivity(intent);
            }
        });

        player_secondary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MusicPlayer.class);
                startActivity(intent);
            }
        });


        Button playNext_button = (Button) findViewById(R.id.car_next);
        Button PlayBackButton = (Button) findViewById(R.id.car_back);

        playNext_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNextSong();
            }
        });
        PlayBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playBackSong();
            }
        });


        mTextDebug = (TextView) findViewById(R.id.car_playet_text);
        Button mPlayButton = (Button) findViewById(R.id.car_player_button1);
        Button mPauseButton = (Button) findViewById(R.id.car_player_button2);
        Button mResetButton = (Button) findViewById(R.id.car_player_button3);
        mSeekbarAudio = (SeekBar) findViewById(R.id.car_player_seek_bar);
        mScrollContainer = (ScrollView) findViewById(R.id.car_player_scroll_view);

        mPauseButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mPlayerAdapter.pause();
                    }
                });
        mPlayButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mPlayerAdapter.play();
                    }
                });
        mResetButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mPlayerAdapter.reset();
                    }
                });
    }

    private void initializePlaybackController() {
        MediaPlayerHolder mMediaPlayerHolder = new MediaPlayerHolder(this);
        Log.d(TAG, "initializePlaybackController: created MediaPlayerHolder");
        mMediaPlayerHolder.setPlaybackInfoListener(new PlaybackListener());
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
            playNextSong();
        }


        @Override
        public void onLogUpdated(String message) {
            if (mTextDebug != null) {
                mTextDebug.append(message);
                mTextDebug.append("\n");
                // Moves the scrollContainer focus to the end.
                mScrollContainer.post(
                        new Runnable() {
                            @Override
                            public void run() {
                                mScrollContainer.fullScroll(ScrollView.FOCUS_DOWN);
                            }
                        });
            }
        }
    }


    int searchSongByEmotion() {
        lastSongPosition = songPosition;
        int newSongPosition = songPosition;
        boolean noEqual = false;

        if (newSongPosition + 1 != songList.size()) {
            newSongPosition++;
        } else newSongPosition = 0;

        int temp = 0;

//            Log.e("SearchingSong", "Telesny tep je " + String.valueOf(heartRate));

        if (heartRate < averageHeartRat - 5) {
            Log.e("SearchingSong", "Cheme zvacsovat ");

            while (!songList.get(newSongPosition).getEmotionStatus().equals("R")) {
//                    Log.e("SearchingSong", "song number " + newSongPosition + " with emotion status: " + songList.get(newSongPosition).getEmotionStatus() + " not fit into R //" + heartRate);

                if (temp == songList.size()) {
                    songPosition += 1;
                    noEqual = true;
                    break;
                }

                if (newSongPosition + 1 != songList.size()) {
                    newSongPosition++;
                    temp++;
                } else newSongPosition = 0;
            }
        } else {
            if (heartRate > averageHeartRat + 5) {
                Log.e("SearchingSong", "Cheme zmensovat ");
//                    Log.e("SearchingSong", String.valueOf(songList.get(newSongPosition).getEmotionStatus().equals("D")));
//                    Log.e("SearchingSong", String.valueOf(songList.get(newSongPosition).getEmotionStatus()));

                while (!songList.get(newSongPosition).getEmotionStatus().equals("D")) {
//                        Log.e("SearchingSong", "song " + songList.get(newSongPosition).getTitle() + " with emotion status: " + songList.get(newSongPosition).getEmotionStatus() + " not fit into D //" + heartRate);

                    if (temp == songList.size()) {
                        songPosition += 1;
                        noEqual = true;
                        break;
                    }

                    if (newSongPosition + 1 != songList.size()) {
                        newSongPosition++;
                        temp++;

                    } else newSongPosition = 0;
                }
            } else {
                while (!songList.get(newSongPosition).getEmotionStatus().equals("N")) {
                    Log.e("SearchingSong", "Ostavame neutral ");

//                        Log.e("SearchingSong", "song number " + newSongPosition + " with emotion status: " + songList.get(newSongPosition).getEmotionStatus() + " not fit into N //" + heartRate);

                    if (temp == songList.size()) {
                        songPosition += 1;
                        noEqual = true;
                        break;
                    }

                    if (newSongPosition + 1 != songList.size()) {
                        newSongPosition++;
                        temp++;

                    } else newSongPosition = 0;
                }
            }
        }

        if (noEqual) {
            Log.e("SearchingSong", "Nenasli sme ziadnu zhodu emocii, vyberame dalsiu piesen v poradi");
            return songPosition += 1;

        } else {
            Log.e("SearchingSong", "Vyberame piesen: " + songList.get(newSongPosition).getTitle() + " - " + songList.get(newSongPosition).getEmotionStatus());
            return newSongPosition;

        }
    }

    private String getEmotionStatusFromFile(int songID) {


        String emotion = "";
        try {
            InputStream inputStream = getApplicationContext().openFileInput("SongEmotions");
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String line = bufferedReader.readLine();
                do {
                    if (line.split(" ")[0].equals(String.valueOf(songID))) {
                        emotion = line.split(" ")[1];
                        break;
                    }
                    line = bufferedReader.readLine();
                } while (line != null);
                inputStream.close();
            }
        } catch (FileNotFoundException e) {
            Log.e("SongAdapter", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("SongAdapter", "Can not read file: " + e.toString());
        }
        return emotion;
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

    public void startCountingHR() {
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

                        heartRateinfo.setText(String.valueOf(polarBroadcastData.hr));
                        hrValues.add(polarBroadcastData.hr);


                        if (!runnableFlag[0]) startCountingHR();
                        runnableFlag[0] = true; // when we started average counting, we can disable start of runnable counter

//                        Log.d(TAG,"HR SenZOR OH1 BROADCAST " +
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
