package com.egrech.app.heartcontrol;

import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import java.io.FileOutputStream;

public class Song {

    private long id;
    private String title;
    private String artist;
    private String emotionStatus;


    public Song(long songID, String songTitle, String songArtist, String eStatus) {
        id=songID;
        title=songTitle;
        artist=songArtist;
        eStatus = emotionStatus;
    }




    public long getID(){
        return id;
    }
    public String getTitle(){
        return title;
    }
    public String getArtist() {
        return artist;
    }
    public String getEmotionStatus(){
        return emotionStatus;
    }
    public void setEmotionStatus(String e) {
        emotionStatus = e;
    }
}
