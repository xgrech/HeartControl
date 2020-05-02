package com.egrech.app.heartcontrol;

import android.content.Intent;
import android.os.Handler;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;

public class Launcher extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        Animation animFadein = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.fade_in);
        Animation rightSlide = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.right_slide);
        Animation leftSlide = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.left_slide);

        findViewById(R.id.tender_view).setAnimation(rightSlide);
        findViewById(R.id.heart_view).setAnimation(leftSlide);
        findViewById(R.id.heart_icon_view).setAnimation(animFadein);

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                Intent intent = new Intent(getApplicationContext(), Menu.class);
//                Intent intent = new Intent(getApplicationContext(), Login.class);
                startActivity(intent);
            }
        }, 1800 );//time in milisecond

    }
}
