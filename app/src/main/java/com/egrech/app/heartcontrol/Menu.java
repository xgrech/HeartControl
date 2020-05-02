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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.UUID;


public class Menu extends AppCompatActivity {

    private final static String TAG = PolarConnection.class.getSimpleName();

    private static final int REQUEST_ENABLE_BT = 356;
    private static final int SET_UP_NEW_USER = 14;
    private static final int RESULT_CANCELED = 357;
    private static final int REQUEST_ENABLE_GPS = 3358;

    String DEVICE_ID = "612F262A"; // or bt address like F5:A7:B8:EF:7A:D1 // TODO replace with your device id

    PolarBleApi api;
    Disposable broadcastDisposable;

    Boolean isBluetoothOn = false;
    Boolean isGPSOn = false;

    // Adresa OH1 senzoru
    // A0:9E:1A:61:2F:26
    // 612F262A

    ImageView heartRateBackground;
    TextView heartRateinfo;
    Button musicPlayer, options, logout, carPlayer;

    private FirebaseAuth mAuth;
    private FirebaseDatabase mFirebaseDatabaseInstance;
    private DatabaseReference mFirebaseDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);


        Animation animFadeinFast = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.fade_in_fast);
        Animation animFadeOut = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.fade_out);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }


//        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
//        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//            Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//            startActivityForResult(gpsOptionsIntent, REQUEST_ENABLE_GPS);
//        }

        checkForPermission();


        musicPlayer = (Button) findViewById(R.id.menu_music_player);
        options = (Button) findViewById(R.id.menu_options);
        logout = (Button) findViewById(R.id.menu_logout);
        heartRateBackground = (ImageView) findViewById(R.id.menu_heart_rate_background);
        heartRateinfo = (TextView) findViewById(R.id.menu_heart_rate);
        carPlayer = (Button) findViewById(R.id.menu_car);

        checkNewUser();


        musicPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), MusicPlayer.class);
                startActivity(intent);
            }
        });

        options.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), UserProfile.class);
                startActivity(intent);
            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getApplicationContext(), Login.class);
                startActivity(intent);
            }
        });

        connectPolarDevice();

        heartRateinfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                heartRateBackground.startAnimation(animFadeinFast);
                connectPolarDevice();
            }
        });


        carPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), CarPlayer.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SET_UP_NEW_USER) {
            if (resultCode == SetNewUser.RESULT_OK) {
                String result = data.getStringExtra("result");
            }
            if (resultCode == SetNewUser.RESULT_CANCELED) {
                Log.e("REsuLT_Activity", "Returned in MENU");
            }
        }

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == SetNewUser.RESULT_OK) {
                isBluetoothOn = true;
            }
            if (resultCode == RESULT_CANCELED) {
                isBluetoothOn = false;
            }
        }
        if (requestCode == REQUEST_ENABLE_GPS) {
            if (resultCode == SetNewUser.RESULT_OK) {
                isGPSOn = true;
            }
            if (resultCode == RESULT_CANCELED) {
                isGPSOn = false;
            }
        }
    }//onActivityResult


    void connectPolarDevice() {
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

    void checkNewUser() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference dbUsersRef = database.getReference("users");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        String newUserFlag = getIntent().getStringExtra("newUserFlag");

        if (Boolean.parseBoolean(newUserFlag)) {
            Intent i = new Intent(this, SetNewUser.class);
            startActivityForResult(i, SET_UP_NEW_USER);
        }
    }

    void checkForPermission() {
        // check for permission
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


}
