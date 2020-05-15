package com.egrech.app.heartcontrol;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import io.reactivex.android.schedulers.AndroidSchedulers;
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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;

public class SportPlayer extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 356;
    private static final int SET_UP_NEW_USER = 14;
    private static final int RESULT_CANCELED = 357;
    private static final int REQUEST_ENABLE_GPS = 3358;

    String DEVICE_ID = ""; // or bt address like F5:A7:B8:EF:7A:D1 // TODO replace with your device id

    PolarBleApi api;
    Disposable broadcastDisposable;

    Boolean isBluetoothOn = false;
    Boolean isGPSOn = false;

    private ImageView playBackButton;
    private ImageView playNextButton;
    private ImageView sportLevelIndicator;

    private ProgressBar progressBar;

    int averageHeartRat = 90; // todo doriesit analyzator priemerneho tepu

    int heartRate;
    int senzorData = 0;

    private ImageView auto_primary;

    private ImageView heartRateBackground;
    private TextView heartRateinfo;

    boolean mode_menu_open = false;

    public static final String TAG = "SportPlayerActivity";

    private SeekBar mSeekbarAudio;
    private PlayerAdapter mPlayerAdapter;
    private boolean mUserIsSeeking = false;

    private TextView player_next_song_title;
    private TextView player_song_title;
    private TextView player_song_artist;
    private TextView player_actual_time;
    private TextView player_song_duration;

    private Button mPauseButton;
    private Button mResetButton;
    private Button sportCreateEmotionList;


    private ConstraintLayout player_next_layout;
    private ConstraintLayout player_back_layout;
    private ConstraintLayout player_song_layout;

    private ArrayList<Song> songList;

    private ImageView player_emotion_indicator;

    int songPosition = -1;
    int nextSong = 0;

    private ArrayList<Integer> playedSongs;

    boolean playEnabled = false;
    boolean playWasToutched = false; // todo

    private Animation animFadein;
    private NumberPicker hrPicker;

    MediaPlayerHolder mMediaPlayerHolder;

    Disposable scanDisposable;

    boolean senzorConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sport_player);

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
        heartRateinfo = (TextView) findViewById(R.id.sport_palyer_heart_rate);
        heartRateinfo.setVisibility(View.GONE);


        songList = new ArrayList<Song>();

        getSongList();

        initializeUI();
        scanForDevice();  // inicializuj pripojenie OH1 senzoru

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

        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
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
        if (playEnabled) {
            player_song_title.setText(songList.get(nextSong).getTitle());
            player_song_artist.setText(songList.get(nextSong).getArtist());

            mPlayerAdapter.loadMedia(ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    songList.get(nextSong).getID()), songList.get(nextSong));

            if (mPlayerAdapter.isPlaying() || lastFinished) {
                mPlayerAdapter.reset();
                mPlayerAdapter.play();
            } else mPlayerAdapter.reset();


            songPosition = nextSong;
            playedSongs.add(songPosition);

            searchSongByEmotion();
        }
    }

    void playBackSong() {
        if (playEnabled) {
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
    }


    private void initializeUI() {

        hrPicker = (NumberPicker) findViewById(R.id.sport_number_picker);

        animFadein = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.fade_in);

        progressBar = (ProgressBar) findViewById(R.id.sport_progress_bar);
        playedSongs = new ArrayList<Integer>();

        player_next_song_title = (TextView) findViewById(R.id.sport_next_song_title);
        player_emotion_indicator = (ImageView) findViewById(R.id.sport_song_indicator);

        player_song_title = (TextView) findViewById(R.id.sport_player_next_song_title);
        player_song_artist = (TextView) findViewById(R.id.sport_player_song_artist);


        player_actual_time = (TextView) findViewById(R.id.sport_actual_song_time);
        player_song_duration = (TextView) findViewById(R.id.sport_song_duration);


//        player_next_layout = (ConstraintLayout) findViewById(R.id.car_player_next_layout);
//        player_back_layout = (ConstraintLayout) findViewById(R.id.car_player_back_layout);
//        player_song_layout = (ConstraintLayout) findViewById(R.id.car_player_song_layout);

//
        auto_primary = (ImageView) findViewById(R.id.sport_player_mode_button);

        playBackButton = (ImageView) findViewById(R.id.sport_play_back);
        playNextButton = (ImageView) findViewById(R.id.sport_play_next);

        sportLevelIndicator = (ImageView) findViewById(R.id.sport_level_indicator);


        ImageView mPlayButton = (ImageView) findViewById(R.id.sport_play);

//        mPauseButton = (Button) findViewById(R.id.car_player_button2);
//        mResetButton = (Button) findViewById(R.id.car_player_button3);

        sportCreateEmotionList = (Button) findViewById(R.id.sport_create_emotion_list);
        sportCreateEmotionList.setVisibility(View.GONE);

        mSeekbarAudio = (SeekBar) findViewById(R.id.sport_seek_bar);


        hrPicker.setMinValue(60);
        hrPicker.setMaxValue(190);
        hrPicker.setValue(90);

        hrPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                averageHeartRat = newVal;
            }
        });

        auto_primary.setOnClickListener(new View.OnClickListener() {
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
                if (playEnabled) {
                    playNextSong(false);
                    playNextButton.setImageResource(R.drawable.forward_button);

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            playNextButton.setImageResource(R.drawable.forward_button_clicked);
                        }
                    }, 200);
                }
            }
        });
        playBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playEnabled) {
                    playBackSong();
                    playBackButton.setImageResource(R.drawable.back_button);

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            playBackButton.setImageResource(R.drawable.back_button_clicked);
                        }
                    }, 200);
                }
            }
        });

        mPlayButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (playEnabled) {
                            if (mPlayerAdapter.isPlaying()) {
                                mPlayerAdapter.pause();
                                mPlayButton.setImageResource(R.drawable.play_stop_button_playing);
                            } else {
                                mPlayerAdapter.play();
                                mPlayButton.setImageResource(R.drawable.play_stop_button);
                            }
                        }
                    }
                });
    }

    private void initializePlaybackController() {
        mMediaPlayerHolder = new MediaPlayerHolder(this);
        Log.d(TAG, "initializePlaybackController: created MediaPlayerHolder");
        mMediaPlayerHolder.setPlaybackInfoListener(new SportPlayer.PlaybackListener());
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

    private void changeSongPosition() {
        int hours = Math.floorDiv((int) mMediaPlayerHolder.getCurrentPosition(), 3600000);
        int minutes = Math.floorDiv((int) mMediaPlayerHolder.getCurrentPosition() % 3600000, 60000);
        int secondes = (int) ((mMediaPlayerHolder.getCurrentPosition() % 36000000) % 60000) / 1000;
        if (hours != 0){
            if (secondes < 10) {
                player_actual_time.setText(String.valueOf(hours + ":0" +minutes+ ":0" + secondes));
            } else player_actual_time.setText(String.valueOf(hours + ":0" +minutes+ ":0" + secondes));
        } else if (secondes < 10) {
            player_actual_time.setText(String.valueOf(minutes+ ":0" + secondes));
        } else player_actual_time.setText(String.valueOf(minutes+ ":" + secondes));
    }


    public class PlaybackListener extends PlaybackInfoListener {

        @Override
        public void onDurationChanged(int duration) {
            mSeekbarAudio.setMax(duration);

            int hours = Math.floorDiv((int) mMediaPlayerHolder.getSongDuration(), 3600000);
            int minutes = Math.floorDiv((int) mMediaPlayerHolder.getSongDuration() % 3600000, 60000);
            int secondes = (int) ((mMediaPlayerHolder.getSongDuration() % 36000000) % 60000) / 1000;

            if (hours != 0){
                if (secondes < 10) {
                    player_song_duration.setText(String.valueOf(hours + ":0" +minutes+ ":0" + secondes));
                } else player_song_duration.setText(String.valueOf(hours + ":0" +minutes+ ":0" + secondes));
            } else if (secondes < 10) {
                player_song_duration.setText(String.valueOf(minutes+ ":0" + secondes));
            } else player_song_duration.setText(String.valueOf(minutes+ ":" + secondes));



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

    @SuppressLint("SetTextI18n")
    int searchSongByEmotion() {
        int newSongPosition = songPosition;
        boolean noEqual = false;
        Log.e("DEBUGG", String.valueOf(songPosition));
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
                    playEnabled = false;

//                    mPauseButton.setVisibility(View.GONE);
//                    mResetButton.setVisibility(View.GONE);


//                    car_player_song_layout.setBackgroundResource(R.drawable.blue_background_warning);
//                    car_player_back_layout.setBackgroundResource(R.drawable.blue_background_warning);
//                    car_player_next_layout.setBackgroundResource(R.drawable.blue_background_warning);

                    createEmotionList();


                    player_song_title.setText("No Songs in Rising Emotion List"); //todo doriesit presmerovanie na emocny zoznam
                    player_song_artist.setVisibility(View.GONE);
                    sportCreateEmotionList.setVisibility(View.VISIBLE);

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
                        playEnabled = false;

                        createEmotionList();
                        player_song_title.setText("No Songs in Rising Emotion List"); //todo doriesit presmerovanie na emocny zoznam
                        player_song_artist.setVisibility(View.GONE);
                        sportCreateEmotionList.setVisibility(View.VISIBLE);

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
                        playEnabled = false;

                        createEmotionList();

                        player_song_title.setText("No Songs in Rising Emotion List"); //todo doriesit presmerovanie na emocny zoznam
                        player_song_artist.setVisibility(View.GONE);
                        sportCreateEmotionList.setVisibility(View.VISIBLE);
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
            playEnabled = false;
            return -1;
        } else {
            Log.e("SearchingSong", "Vyberame piesen: " + songList.get(newSongPosition).getTitle() + " - " + songList.get(newSongPosition).getEmotionStatus());
            nextSong = newSongPosition;

            String songEmotionStatus = songList.get(nextSong).getEmotionStatus();
            player_next_song_title.setText(songList.get(nextSong).getTitle());

            if (songEmotionStatus.equals("R")) {
                player_emotion_indicator.setImageResource(R.drawable.note_green);
                player_next_song_title.setTextColor(getResources().getColor(R.color.green));
            } else if (songEmotionStatus.equals("D")) {
                player_emotion_indicator.setImageResource(R.drawable.note_red);
                player_next_song_title.setTextColor(getResources().getColor(R.color.red));
            } else {
                player_emotion_indicator.setImageResource(R.drawable.note_yellow);
                player_next_song_title.setTextColor(getResources().getColor(R.color.gold));
            }

            return 1;
        }
    }


    private void createEmotionList() {
        sportCreateEmotionList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayerAdapter.release();
                Intent intent = new Intent(getApplicationContext(), MusicPlayer.class);
                startActivity(intent);
            }
        });
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

            if(hrValues.size() != 0)
            heartRate = hrCount / hrValues.size();


            heartRateinfo.startAnimation(animFadein);

            Log.e("DEBUGGg", String.valueOf(songPosition));

            if (searchSongByEmotion() != -1) {
                if (!playEnabled) {

                    mPlayerAdapter.loadMedia(ContentUris.withAppendedId(
                            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            songList.get(nextSong).getID()), songList.get(nextSong));

                    songPosition = nextSong;
                    playedSongs.add(songPosition);

//                    player_actual_time.setVisibility(View.VISIBLE);
//                    player_song_duration.setVisibility(View.VISIBLE);

                    playEnabled = true;

                    player_song_title.setText(songList.get(nextSong).getTitle());
                    player_song_artist.setText(songList.get(nextSong).getArtist());

                    searchSongByEmotion();
                }
            }


            //Log.e("HEARTRATE", "Actual avaregae measurment of Heart Rate is: "+heartRate);
            hrValues.clear();
            if(senzorConnected) {
                restartCountingHR();
            } else {
                progressBar.setVisibility(View.VISIBLE);
                heartRateinfo.setVisibility(View.GONE);
                scanForDevice();
            }
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


    private void updateLevelIndicator(int hrData) {
        ConstraintLayout sport_cinka = findViewById(R.id.sport_cinka);

        if (hrData < averageHeartRat - 5) {
            sportLevelIndicator.setImageResource(R.drawable.sport_up);
            sport_cinka.setBackgroundResource(R.drawable.sport_cinka_blue);
        } else if (hrData > averageHeartRat + 5) {
            sportLevelIndicator.setImageResource(R.drawable.sport_down);
            sport_cinka.setBackgroundResource(R.drawable.sport_cinka_red);
        } else {
            sportLevelIndicator.setImageResource(R.drawable.sport_ok);
            sport_cinka.setBackgroundResource(R.drawable.sport_cinka_green);
        }

    }

    @SuppressLint("SetTextI18n")
    void scanForDevice() {

        progressBar.setVisibility(View.VISIBLE);

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

        Log.d(TAG, "Start ScANN SesSiOn");

        if (scanDisposable == null) {
            scanDisposable = api.searchForDevice().observeOn(AndroidSchedulers.mainThread()).subscribe(
                    new Consumer<PolarDeviceInfo>() {
                        @Override
                        public void accept(PolarDeviceInfo polarDeviceInfo) throws Exception {
                            Log.d(TAG, "BLE device found id: " + polarDeviceInfo.deviceId + " address: " + polarDeviceInfo.address + " rssi: " + polarDeviceInfo.rssi + " name: " + polarDeviceInfo.name + " isConnectable: " + polarDeviceInfo.isConnectable);

                            String deviceNameMark = String.valueOf(polarDeviceInfo.name).split(" ")[0];
//                            Log.d(TAG, "Device Mark is: " + deviceNameMark);

                            if (deviceNameMark.equals("Polar")) {
                                DEVICE_ID = polarDeviceInfo.address;

                                scanDisposable.dispose();
                                senzorConnected = true;
                                connectPolarDevice();
                            }
                        }
                    },
                    new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            Log.d(TAG, "scannnnnnn" + throwable.getLocalizedMessage());
                        }
                    },
                    new Action() {
                        @Override
                        public void run() throws Exception {
                            Log.d(TAG, "complete");
                        }
                    }
            );
        } else {
            scanDisposable.dispose();
            scanDisposable = null;
        }
    }       // implementacia autoscanu a autoconnectu na senzor


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

            @SuppressLint("SetTextI18n")
            @Override
            public void deviceDisconnected(PolarDeviceInfo polarDeviceInfo) {
                Log.d(TAG, "DISCONNECTED: " + polarDeviceInfo.deviceId);
                senzorConnected = false;
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
                        if(senzorConnected) {
                            progressBar.setVisibility(View.GONE);
                            heartRateinfo.setVisibility(View.VISIBLE);
                            player_song_title.setVisibility(View.VISIBLE);

                            senzorData = polarBroadcastData.hr;

                            updateLevelIndicator(senzorData);
                            heartRateinfo.setText(String.valueOf(senzorData));

                            hrValues.add(senzorData);
                            changeSongPosition();

                            if (!runnableFlag[0]) startCountingHR(polarBroadcastData.hr);
                            runnableFlag[0] = true; // when we started average counting, we can disable start of runnable counter
                        } else stopCountingHR();


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
                        Log.d(TAG, "complete SenzorOFF");
                    }
                }
        );
    }
}
