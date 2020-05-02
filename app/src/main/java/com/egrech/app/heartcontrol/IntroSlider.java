package com.egrech.app.heartcontrol;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class IntroSlider extends AppIntro {

    SharedPreferences prefs;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_intro_slider);



        addSlide(AppIntroFragment.newInstance("Welcome","Tender heart is music application with beautiful design which perform powerfull functions.",
                R.drawable.music_icon, ContextCompat.getColor(getApplicationContext(),R.color.colorPrimary)));
        addSlide(AppIntroFragment.newInstance("Heart monitor","Get information about your heart rate and use it for playing music. Choose favorite peaces and then let it on us.",
                R.drawable.cardio,ContextCompat.getColor(getApplicationContext(),R.color.gold)));
        addSlide(AppIntroFragment.newInstance("Inteligent content","Your heart rate information powers inteligent algorithms which counting perfect music in each situation. Your heart sings for you.",
                R.drawable.ai,ContextCompat.getColor(getApplicationContext(),R.color.black)));


        prefs = getSharedPreferences("nbbbRepet",Context.MODE_PRIVATE);
        editor = prefs.edit();
        editor.putInt("nbbbRepet", 1);
        editor.apply();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra("EXTRA_RUN_TOKEN", 1);
        startActivity(intent);
    }
}
