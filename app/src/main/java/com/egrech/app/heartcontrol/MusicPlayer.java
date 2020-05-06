package com.egrech.app.heartcontrol;

import android.Manifest;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.DatabaseUtils;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.View;


import android.widget.MediaController.MediaPlayerControl;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MusicPlayer extends AppCompatActivity implements MediaPlayerControl {
    private ArrayList<Song> songList;
    private ListView songView;

    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound = false;

    private Button shuffle_play;
    private Button end_all;

    private MusicController controller;

    TextView currentSongTitle;
    TextView currentSongArtist;
    TextView pickSongInfo; // simple text to pick a song in music player

    View lastSongView;
    int lastPickedPosition = 0;

    int topSongViewPosition = 0;
    int bottomSongViewPosition = 7;

    private boolean paused = false, playbackPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        // check for permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted so we ask for it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    12);
        }

        pickSongInfo = (TextView) findViewById(R.id.pick_song_info_text);

        shuffle_play = (Button) findViewById(R.id.shuffle_button);
        shuffle_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                musicSrv.setShuffle();
            }
        });

        end_all = (Button) findViewById(R.id.end_button);
        end_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(new Intent(getApplicationContext(), MusicService.class));
                musicSrv = null;
                System.exit(0);

                Intent intent = new Intent(getApplicationContext(), Menu.class);
                startActivity(intent);
            }
        });

        songView = (ListView) findViewById(R.id.song_list);
        songList = new ArrayList<Song>();

        getSongList();

        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });

        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);

        currentSongTitle = (TextView) findViewById(R.id.current_song_title);
        currentSongArtist = (TextView) findViewById(R.id.current_song_artist) ;

        setController();
    }

    @Override
    public void onBackPressed() {
        System.exit(0);
        Intent intent = new Intent(getApplicationContext(), Menu.class);
        startActivity(intent);
    }

    private void readFromFile(Context context) {
        Log.d("FILEDATA", "Reeading Data from file");

        try {
            InputStream inputStream = context.openFileInput("SongEmotions");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                String line = bufferedReader.readLine();
                while(line != null){
                    Log.d("FILEDATA", line);
                    line = bufferedReader.readLine();
                }

                inputStream.close();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }
    }

    public int getSongPosition() {
        int i = 0;
        for (i = 0; i < songList.size(); i++) {
            if(musicSrv.getCurrentSong().getTitle().equals(songList.get(i).getTitle())) return i;
        }
        return i;
    }

    private void playNext() {
        musicSrv.playNext();
        if (playbackPaused) {
            setController();
            playbackPaused = false;
        }
        controller.show(0);

        currentSongTitle.setText(musicSrv.getCurrentSong().getTitle());
        currentSongArtist.setText(musicSrv.getCurrentSong().getArtist());



        View actualSongView = getViewByPosition(getSongPosition(),songView);
        actualSongView.setBackgroundColor(getApplicationContext().getResources().getColor(R.color.gray_transparent));

        if (lastSongView != null) {
            lastSongView.setBackgroundColor(getApplicationContext().getResources().getColor(R.color.default_background));
        }
        lastSongView = actualSongView;


        if (getSongPosition() > bottomSongViewPosition) {
            bottomSongViewPosition += 1;
            songView.smoothScrollToPosition(bottomSongViewPosition);
        }
    }

    private void playPrev() {
        musicSrv.playPrev();
        if (playbackPaused) {
            setController();
            playbackPaused = false;
        }
        controller.show(0);

        currentSongTitle.setText(musicSrv.getCurrentSong().getTitle());
        currentSongArtist.setText(musicSrv.getCurrentSong().getArtist());


        View actualSongView = getViewByPosition(getSongPosition(),songView);
        actualSongView.setBackgroundColor(getApplicationContext().getResources().getColor(R.color.gray_transparent));

        if (lastSongView != null) {
            lastSongView.setBackgroundColor(getApplicationContext().getResources().getColor(R.color.default_background));
        }
        lastSongView = actualSongView;


//        if (getSongPosition() < topSongViewPosition) {
//            topSongViewPosition -= 1;
//            songView.smoothScrollToPosition(topSongViewPosition);
//        }
    }

    public View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }


    public void songPicked(View view) {
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        Log.e("PLAYINGSONG", view.getTag().toString());
        musicSrv.playSong();
        if (playbackPaused) {
            setController();
            playbackPaused = false;
        }
        pickSongInfo.setVisibility(View.GONE);
        setController();
        controller.show(0);

        view.setBackgroundColor(view.getResources().getColor(R.color.gray_transparent));
        if (lastSongView != null) {
            lastSongView.setBackgroundColor(view.getResources().getColor(R.color.default_background));
        }
        lastSongView = view;

        currentSongTitle.setText(musicSrv.getCurrentSong().getTitle());
        currentSongArtist.setText(musicSrv.getCurrentSong().getArtist());
    }

    private void setController() {
        controller = new MusicController(this);

        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });

        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.music_controller_view));
        controller.setEnabled(true);


    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicSrv = null;
        super.onDestroy();
    }


    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }


    public void updateAlbumData() {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;


        ContentValues test_values = new ContentValues();
        test_values.put(MediaStore.Audio.Media.ALBUM, "Album is emotion info");
        String albumName = "emotion_up";
        int res = musicResolver.update(musicUri, test_values, MediaStore.Audio.Media.ALBUM + "= ?", new String[]{albumName});  // works fine!

    }


    public void dumpCursorByAlbumId(String id) {
        ContentResolver musicResolver = getContentResolver();

        String[] stringArray = {id};
        final Cursor mCursor = musicResolver.query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, // Uri
                new String[]{                                 // String[] projection (columns)
                        MediaStore.Audio.Albums._ID,
                        MediaStore.Audio.Albums.ALBUM_ART,
                        MediaStore.Audio.Albums.ALBUM,
                        MediaStore.Audio.Albums.ARTIST,
                        MediaStore.Audio.Albums.NUMBER_OF_SONGS,
                        MediaStore.Audio.Albums.ALBUM_KEY
                },
                MediaStore.Audio.Albums._ID + "=?",           // String selection
                stringArray,                                  // String[] selectionArgs
                null                                         // sortOrder
        );

        if (mCursor.moveToFirst()) {
            // dump each row in the cursor
            // for(int i=0; i <  stringArray.length; i++){
            DatabaseUtils.dumpCursor(mCursor);
            // mCursor.moveToNext();
            //}
            mCursor.close();
        } else {
            mCursor.close();
        }
    }

    public void dumpCursorBySongId(String id) {
        ContentResolver musicResolver = getContentResolver();

        String[] stringArray = {id};
        final Cursor mCursor = musicResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, // Uri
                new String[]{                                 // String[] projection (columns)
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.ALBUM_ID,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.ARTIST_ID,
                        MediaStore.Audio.Media.DISPLAY_NAME,
                },
                MediaStore.Audio.Albums._ID + "=?",           // String selection
                stringArray,                                  // String[] selectionArgs
                null                                         // sortOrder
        );

        if (mCursor.moveToFirst()) {
            // dump each row in the cursor
            // for(int i=0; i <  stringArray.length; i++){
            DatabaseUtils.dumpCursor(mCursor);
            // mCursor.moveToNext();
            //}
            mCursor.close();
        } else {
            mCursor.close();
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
    }

    public void playWithAlbum(Cursor musicCursor) {

        ContentResolver musicResolver = getContentResolver();
        Uri baseUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
//        ContentValues test_values = new ContentValues();

//        test_values.put(MediaStore.Audio.Media.ALBUM, "Emotions");

//            String baseUri = musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA));
//            Uri baseUriOriginal = Uri.parse("file:///"+baseUri);
//
        String song_name = musicCursor.getString(musicCursor.getColumnIndex
                (MediaStore.Audio.Media._ID));
        baseUri = Uri.withAppendedPath(baseUri, "" + song_name);

        Log.e("INFODATA", String.valueOf(baseUri));


//            content://media/external/audio/media/14
//            content://media/external/audio/media/storage/emulated/0/Samsung/Music/Over_the_Horizon.mp3
//            Log.e("INFODATA", String.valueOf(baseUri));


//            int res = musicResolver.update(baseUri, test_values, MediaStore.Audio.Media.DISPLAY_NAME + "= ?", new String[]{song_name});  // works fine!

//            musicResolver.update(
//                    MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
//                    values,
//                    MediaStore.Audio.Albums.ALBUM_ID + "=" + 16,
//                    null);
//
        Uri songUri = Uri.parse("content://media/external/audio/media");
//        musicResolver.delete(ContentUris.withAppendedId(songUri, 2256), null, null);
//


        ContentValues insertionValues = new ContentValues();
        insertionValues.put(MediaStore.Audio.Media.ALBUM, "Emotions");

        musicResolver.update(ContentUris.withAppendedId(songUri, 2255), insertionValues, null, null);  // works fine!

//        --funkcne
//        Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
//        musicResolver.delete(ContentUris.withAppendedId(albumArtUri, 4), null, null);
//        ContentValues insertionValues = new ContentValues();
//        insertionValues.put("_data", "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1587982388914");
//        insertionValues.put("album_id", 4);
//        getApplicationContext().getContentResolver().insert(albumArtUri, insertionValues);


        Log.e("INFODATARES", musicCursor.getString(musicCursor.getColumnIndex
                (MediaStore.Audio.Media.ALBUM_ID)));


//        dumpCursorByAlbumId(String.valueOf(musicCursor.getString(musicCursor.getColumnIndex
//                (MediaStore.Audio.Media.ALBUM_ID))));

        dumpCursorBySongId(String.valueOf(musicCursor.getString(musicCursor.getColumnIndex
                (MediaStore.Audio.Media._ID))));


    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (paused) {
            setController();
            paused = false;
        }
    }

    @Override
    protected void onStop() {
        controller.hide();
        super.onStop();
    }

    @Override
    public void pause() {
        playbackPaused = true;
        musicSrv.pausePlayer();
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);
    }

    @Override
    public void start() {
        musicSrv.go();
    }

    @Override
    public int getDuration() {
        if (musicSrv != null &&
                musicBound &&
                musicSrv.isPng())
            return musicSrv.getDur();
        else return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (musicSrv != null && musicBound && musicSrv.isPng())
            return musicSrv.getPosn();
        else return 0;
    }

    @Override
    public boolean isPlaying() {
        if (musicSrv != null && musicBound)
            return musicSrv.isPng();
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
}
