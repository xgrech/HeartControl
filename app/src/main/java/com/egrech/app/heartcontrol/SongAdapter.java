package com.egrech.app.heartcontrol;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class SongAdapter extends BaseAdapter {

    private ArrayList<Song> songs;
    private LayoutInflater songInf;
    public Spinner emotion_spinner;

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getItemId(int arg0) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //map to song layout
        @SuppressLint("ViewHolder") LinearLayout songLay = (LinearLayout) songInf.inflate
                (R.layout.song, parent, false);
        //get title and artist views

        TextView songView = (TextView) songLay.findViewById(R.id.car_player_song_title);
        TextView artistView = (TextView) songLay.findViewById(R.id.song_artist);
        ImageView noteView = (ImageView) songLay.findViewById(R.id.song_note_view);
        //get song using position
        Song currSong = songs.get(position);
        //get title and artist strings
        songView.setText(currSong.getTitle());
        artistView.setText(currSong.getArtist());
        //set position as tag
        songLay.setTag(position);


        //nastavujeme emocny spinner
        Spinner emotion_spinner = songLay.findViewById(R.id.song_emotion_spinner);
        ArrayAdapter<CharSequence> emotion_spinner_adapter = ArrayAdapter.createFromResource(songLay.getContext(), R.array.emotion_values
                , android.R.layout.simple_spinner_item);
        emotion_spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        emotion_spinner.setAdapter(emotion_spinner_adapter);

        // koniec nastavenia emocny spinner

        String emo = getEmotionStatusFromFile(currSong, songLay);
        if (emo.equals("R")) {
            songView.setTextColor(songLay.getResources().getColor(R.color.green));
            noteView.setImageResource(R.drawable.note_green);

            emotion_spinner.setSelection(1);
        }
        if (emo.equals("N")) {
            songView.setTextColor(songLay.getResources().getColor(R.color.gold));
            noteView.setImageResource(R.drawable.note_yellow);

            emotion_spinner.setSelection(2);
        }
        if (emo.equals("D")) {
            songView.setTextColor(songLay.getResources().getColor(R.color.red));
            noteView.setImageResource(R.drawable.note_red);
            emotion_spinner.setSelection(3);
        }

        //nastavujeme listener na emocny spinner

        emotion_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int spnPosition, long id) {
                Log.e("SpinnerTest", "Clicked song is " + currSong.getTitle() + " on position " + spnPosition);
                // po kliknuti na spinner emocii vieme updatnut data na tomto mieste

                if (spnPosition == 0) {
                    String token = "delete";
                    writeSongEmotion(currSong, songLay, token);
                    songView.setTextColor(songLay.getResources().getColor(R.color.black));
                    noteView.setImageResource(R.drawable.note);
                }

                if (spnPosition == 1) {
                    String token = " R";
                    writeSongEmotion(currSong, songLay, token);
                    songView.setTextColor(songLay.getResources().getColor(R.color.green));
                    noteView.setImageResource(R.drawable.note_green);
                }
                if (spnPosition == 2) {
                    String token = " N";
                    writeSongEmotion(currSong, songLay, token);
                    songView.setTextColor(songLay.getResources().getColor(R.color.gold));
                    noteView.setImageResource(R.drawable.note_yellow);
                }
                if (spnPosition == 3) {
                    String token = " D";
                    writeSongEmotion(currSong, songLay, token);
                    songView.setTextColor(songLay.getResources().getColor(R.color.red));
                    noteView.setImageResource(R.drawable.note_red);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        return songLay;
    }

    private String getEmotionStatusFromFile(Song currSong, LinearLayout songLay) {
        String emotion = "";
        try {
            InputStream inputStream = songLay.getContext().openFileInput("SongEmotions");
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String line = bufferedReader.readLine();
                do {
                    if (line.split(" ")[0].equals(String.valueOf(currSong.getID()))) {
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

    private void writeSongEmotion(Song currSong, LinearLayout songLay, String emotionToken) {
        int replaceFlag = 0;

        // najprv zistim ci sa v subore nachadza piesen s priradenim emocneho tokenu a nastavim replaceFlag na true ak ano
        try {
            InputStream inputStream = songLay.getContext().openFileInput("SongEmotions");
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                int countLine = 0;

                String line = bufferedReader.readLine();
                do {
                    if (line.split(" ")[0].equals(String.valueOf(currSong.getID()))) {
                        replaceFlag = 1;
                        Log.e("FoundID", "Found song ID on line " + countLine);
                        break;
                    }
                    line = bufferedReader.readLine();
                    countLine++;
                } while (line != null);
                inputStream.close();
            }
        } catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        // nasledne ked je replace flag false len zapisem nove pravidlo s ID piesne a emotionTokenom alebo premazem stare
        if (replaceFlag == 0 && !emotionToken.equals("delete")) {
            try {
                FileOutputStream fOut = songLay.getContext().openFileOutput("SongEmotions", Context.MODE_APPEND);
                String str = String.valueOf(currSong.getID() + emotionToken + "\n\r");
                fOut.write(str.getBytes());
                fOut.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                // input the (modified) file content to the StringBuffer "input"
                InputStream inputStream = songLay.getContext().openFileInput("SongEmotions");
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader file = new BufferedReader(inputStreamReader);
                StringBuilder inputBuffer = new StringBuilder();
                String line;

                while ((line = file.readLine()) != null) {
                    Log.e("CompareLines", line.split(" ")[0] + " is compered to " + String.valueOf(currSong.getID()));
                    if (line.split(" ")[0].equals(String.valueOf(currSong.getID()))) {
                        Log.e("LINECHANGE", "Changing line " + line + " for new one: " + String.valueOf(currSong.getID() + emotionToken));

                        // pokial nenastavujeme rezim ze nie je nic vybrate tak mozeme nastavit nove pravidlo v subore
                        if(!emotionToken.equals("delete")) {
                            inputBuffer.append(String.valueOf(currSong.getID() + emotionToken));
                        }
                    } else {
                        inputBuffer.append(line);
                    }
                    inputBuffer.append('\n');
                }
                file.close();

                FileOutputStream fOut = songLay.getContext().openFileOutput("SongEmotions", Context.MODE_PRIVATE);
                fOut.write(inputBuffer.toString().getBytes());
                fOut.close();

            } catch (Exception e) {
                System.out.println("Problem reading file.");
            }
        }
        currSong.setEmotionStatus(emotionToken);
    }


    public SongAdapter(Context c, ArrayList<Song> theSongs) {
        songs = theSongs;
        songInf = LayoutInflater.from(c);
    }


}
