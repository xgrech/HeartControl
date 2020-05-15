package com.egrech.app.heartcontrol;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private int height, width;
    private GestureDetector mGestureDetector;
    private ImageView music_shade, heartrate_shade, stats_shade, settings_shade, heart_default_icon, heart_rain, music_icon, heart_icon, stats_icon, settings_icon;

    Animation animFadein;
    Animation fadeInFast;
    Animation fadeOut;

    @SuppressLint("ClickableViewAccessibility")
    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences preferences = MainActivity.this.getSharedPreferences("nbbbRepet", MODE_PRIVATE);
        int value = preferences.getInt("nbbbRepet", 0);

        if(value<1)
        {
            Intent intent = new Intent(getApplicationContext(), IntroSlider.class);
            startActivity(intent);
        }

//      deklaracie premennych
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

        height = displaymetrics.heightPixels;
        width = displaymetrics.widthPixels;

//        music_shade = (ImageView) findViewById(R.id.music_shade);
//        heartrate_shade = (ImageView) findViewById(R.id.heart_shade);
//        stats_shade = (ImageView) findViewById(R.id.stat_shade);
//        settings_shade = (ImageView) findViewById(R.id.settings_shade);

        heart_default_icon = (ImageView) findViewById(R.id.default_heart_icon);
        heart_rain = (ImageView) findViewById(R.id.rain_heart_icon);


        music_icon = (ImageView) findViewById(R.id.mm);
        heart_icon = (ImageView) findViewById(R.id.hh);
        stats_icon = (ImageView) findViewById(R.id.ss);
        settings_icon = (ImageView) findViewById(R.id.oo);


        mGestureDetector = new GestureDetector(this, new MyGestureListener());

        animFadein = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.fade_in);
        fadeInFast = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.fade_in_fast);
        fadeOut = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.fade_out);
//        koniec deklaracie premennych

        fadeOut.setFillAfter(true);
        fadeInFast.setFillAfter(true);
        findViewById(R.id.main_menu).setOnTouchListener(touchListener);
//        heart_default_icon.setAnimation(animFadein);
//        settings_shade.startAnimation(animFadein);
//        stats_shade.startAnimation(animFadein);
//        heartrate_shade.startAnimation(animFadein);
//        music_shade.startAnimation(animFadein);
    }
    View.OnTouchListener touchListener = new View.OnTouchListener() {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            if (event.getAction() == MotionEvent.ACTION_UP) {
                music_shade.setVisibility(View.GONE);
                heartrate_shade.setVisibility(View.GONE);
                stats_shade.setVisibility(View.GONE);
                settings_shade.setVisibility(View.GONE);
                heart_default_icon.setVisibility(View.VISIBLE);
                heart_rain.setVisibility(View.GONE);

                music_icon.startAnimation(fadeInFast);
                heart_icon.startAnimation(fadeInFast);
                settings_icon.startAnimation(fadeInFast);
                stats_icon.startAnimation(fadeInFast);

                if ((event.getX() < width / 2) && (event.getY() < height / 2)) {
                    openMusicPlayer();
                }
                if ((event.getX() >= width / 2) && (event.getY() < height / 2)) {
                    polarSensorActivity();
                }
//                if ((event.getX() < width / 2) && (event.getY() >= height / 2)) {
//                }
                if ((event.getX() > width / 2) && (event.getY() >= height / 2)) {
//                    makeConnection();
                }
            }
            // pass the events to the gesture detector
            // a return value of true means the detector is handling it
            // a return value of false means the detector didn't
            // recognize the event
            return mGestureDetector.onTouchEvent(event);
        }
    };

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        public final void on() {
            music_shade.setVisibility(View.VISIBLE);
            heartrate_shade.setVisibility(View.VISIBLE);
            stats_shade.setVisibility(View.VISIBLE);
            settings_shade.setVisibility(View.VISIBLE);
        }
        public final void off() {
            music_shade.setVisibility(View.GONE);
            heartrate_shade.setVisibility(View.GONE);
            stats_shade.setVisibility(View.GONE);
            settings_shade.setVisibility(View.GONE);
            heart_default_icon.setVisibility(View.VISIBLE);
            heart_rain.setVisibility(View.GONE);
        }
        private int handleAction(MotionEvent e) {
            if ((e.getX() < width / 2) && (e.getY() < height / 2)) {
                return 1;
            }
            if ((e.getX() >= width / 2) && (e.getY() < height / 2)) {
                return 2;
            }
            if ((e.getX() < width / 2) && (e.getY() >= height / 2)) {
                return 3;
            }
            if ((e.getX() > width / 2) && (e.getY() >= height / 2)) {
                return 4;
            }
            return 0;
        }
        @Override
        public boolean onDown(MotionEvent event) {
            int x = handleAction(event);
            switch (x) {
                case 1: {
                    on();
                    music_shade.setVisibility(View.GONE);
                    heart_icon.startAnimation(fadeOut);
                    settings_icon.startAnimation(fadeOut);
                    stats_icon.startAnimation(fadeOut);
                    break;
                }
                case 2: {
                    on();
                    heartrate_shade.setVisibility(View.GONE);
                    music_icon.startAnimation(fadeOut);
                    settings_icon.startAnimation(fadeOut);
                    stats_icon.startAnimation(fadeOut);
                    break;
                }
                case 3: {
                    on();
                    stats_shade.setVisibility(View.GONE);
                    music_icon.startAnimation(fadeOut);
                    heart_icon.startAnimation(fadeOut);
                    settings_icon.startAnimation(fadeOut);

                    break;
                }
                case 4: {
                    on();
                    settings_shade.setVisibility(View.GONE);
                    music_icon.startAnimation(fadeOut);
                    heart_icon.startAnimation(fadeOut);
                    stats_icon.startAnimation(fadeOut);

                    break;
                }
            }
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Log.i("TAG", "onSingleTapConfirmed: ");

            switch (handleAction(e)) {
                case 1: {
                    openMusicPlayer();
                    break;
                }
                case 2: {
                    makeSecondConnection();
                    break;
                }
                case 4: {
//                    makeConnection();
                    break;
                }
            }
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {

            switch (handleAction(e)) {
                case 1: {
                    openMusicPlayer();
                    break;
                }
                case 2: {
                    makeSecondConnection();
                    break;
                }
                case 4: {
//                    makeConnection();
                    break;
                }
            }
            Log.i("TAG", "onLongPress: ");
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.i("TAG", "onDoubleTap: ");
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            Log.i("TAG", "onScrolll: ");
            off();
            int x = handleAction(e2);
            switch (x) {
                case 1: {
                    on();
                    music_shade.setVisibility(View.GONE);
                    break;
                }
                case 2: {
                    on();
                    heartrate_shade.setVisibility(View.GONE);

                    heart_icon.startAnimation(fadeInFast);
                    break;
                }
                case 3: {
                    on();
                    stats_shade.setVisibility(View.GONE);

                    stats_icon.startAnimation(fadeInFast);
                    break;
                }
                case 4: {
                    on();
                    settings_shade.setVisibility(View.GONE);

                    settings_icon.startAnimation(fadeInFast);
                    break;
                }
            }
            return true;
        }
        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            Log.d("TAG", "onFling: ");
            return true;
        }
    }


    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;

    public boolean checkPermissionREAD_EXTERNAL_STORAGE(
            final Context context) {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        (Activity) context,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    showDialog("External storage", context,
                            Manifest.permission.READ_EXTERNAL_STORAGE);

                } else {
                    ActivityCompat
                            .requestPermissions(
                                    (Activity) context,
                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                }
                return false;
            } else {
                return true;
            }

        } else {
            return true;
        }
    }

    public void showDialog(final String msg, final Context context,
                           final String permission) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setCancelable(true);
        alertBuilder.setTitle("Permission necessary");
        alertBuilder.setMessage(msg + " permission is necessary");
        alertBuilder.setPositiveButton(android.R.string.yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions((Activity) context,
                                new String[]{permission},
                                MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                    }
                });
        AlertDialog alert = alertBuilder.create();
        alert.show();
    }



    void polarSensorActivity() {
        Intent intent = new Intent(this, PolarConnection.class);
        startActivity(intent);
    }

    void makeSecondConnection() {
//        Intent intent = new Intent(this, GattClientMainActivity.class);
//        Intent intent = new Intent(this, DeviceScanActivity.class);
//        startActivity(intent);
    }


    void openMusicPlayer() {
        if (checkPermissionREAD_EXTERNAL_STORAGE(this)) {
            Intent intent = new Intent(this, MusicPlayer.class);
            startActivity(intent);
        }

    }
}
