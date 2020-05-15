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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.UUID;


public class Menu extends AppCompatActivity {

    //    private final static String TAG = PolarConnection.class.getSimpleName();
    private final static String TAG = "MenuActivity";

    private static final int REQUEST_ENABLE_BT = 356;
    private static final int SET_UP_NEW_USER = 14;
    private static final int RESULT_CANCELED = 357;
    private static final int REQUEST_ENABLE_GPS = 3358;

    String DEVICE_ID = ""; // or bt address like F5:A7:B8:EF:7A:D1 // TODO replace with your device id
    int heartRate;
    PolarBleApi api;
    Disposable scanDisposable;
    Disposable broadcastDisposable;

    Boolean isBluetoothOn = false;
    Boolean isGPSOn = false;

    // Adresa OH1 senzoru
    // A0:9E:1A:61:2F:26
    // 612F262A

    ImageView heartRateBackground;
    TextView heartRateinfo;
    Button musicPlayer, options, logout, carPlayer, sportPlayer, sleepPlayer;
    ConstraintLayout carPlayerLayout, musicPlayerLayout, sleepPlayerLayout, sportPlayerLayout;
    TextView carPlayerLayoutText, sportPlayerLayoutText;
    ImageView carPlayerLayoutIcon, sportPlayerLayoutIcon;

    private FirebaseAuth mAuth;
    private FirebaseDatabase mFirebaseDatabaseInstance;
    private DatabaseReference mFirebaseDatabase;

    private ProgressBar scanProgressBar;
    private TextView scanText;


    boolean hrSenzorActive = false;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        inicializeTermination();

        checkNewUser();


        inicializeUI();
        setUpListeners();

        checkBluetoothAndGPS();
        scanForDevice();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkBluetoothAndGPS();
    }

    @SuppressLint("SetTextI18n")
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

                checkForPermission();

                LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

                    scanText.setText("Povolte  GPS služby");
                    scanText.setTextColor(getApplicationContext().getResources().getColor(R.color.red));

                    scanText.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(gpsOptionsIntent, REQUEST_ENABLE_GPS);
                        }
                    });
                }


                isBluetoothOn = true;
            }
            if (resultCode == RESULT_CANCELED) {
                isBluetoothOn = false;

                hrSenzorActive = false;
                scanText.setText("Povolte  Bluetooth");
                scanText.setTextColor(getApplicationContext().getResources().getColor(R.color.red));
            }
        }
        if (requestCode == REQUEST_ENABLE_GPS) {
            if (resultCode == SetNewUser.RESULT_OK) {
                isGPSOn = true;
            }
            if (resultCode == RESULT_CANCELED) {
                isGPSOn = false;

                hrSenzorActive = false;
                scanText.setText("Povolte  GPS služby");
                scanText.setTextColor(getApplicationContext().getResources().getColor(R.color.red));
            }
        }
    }//onActivityResult


    @SuppressLint("SetTextI18n")
    private void inicializeUI() {
        musicPlayerLayout = (ConstraintLayout) findViewById(R.id.menu_music_player_layout);
        sleepPlayerLayout = (ConstraintLayout) findViewById(R.id.menu_sleep_player_layout);


        carPlayerLayout = (ConstraintLayout) findViewById(R.id.menu_car_player_layout);
        carPlayerLayoutIcon = (ImageView) findViewById(R.id.menu_car_player_layout_icon);
        carPlayerLayoutText = (TextView) findViewById(R.id.menu_car_player_layout_text);

        sportPlayerLayout = (ConstraintLayout) findViewById(R.id.menu_sport_player_layout);
        sportPlayerLayoutText = (TextView) findViewById(R.id.menu_sport_player_layout_text);
        sportPlayerLayoutIcon = (ImageView) findViewById(R.id.menu_sport_player_layout_icon);

        musicPlayer = (Button) findViewById(R.id.menu_music_player);
        options = (Button) findViewById(R.id.menu_options);
        logout = (Button) findViewById(R.id.menu_logout);

        heartRateinfo = (TextView) findViewById(R.id.menu_heart_rate);
        carPlayer = (Button) findViewById(R.id.menu_car);
        sportPlayer = (Button) findViewById(R.id.menu_sport);

        sleepPlayer = (Button) findViewById(R.id.menu_sleep);
        sleepPlayer.setVisibility(View.VISIBLE);

        scanProgressBar = (ProgressBar) findViewById(R.id.menu_scan_progressBar);

        scanText = (TextView) findViewById(R.id.menu_scan_text);
        scanText.setVisibility(View.VISIBLE);

        Log.e("initUI", String.valueOf(hrSenzorActive));
        if (hrSenzorActive) {
            carPlayer.setEnabled(true);
            sportPlayer.setEnabled(true);

            carPlayerLayoutIcon.setVisibility(View.VISIBLE);
            carPlayerLayoutText.setText("");

            sportPlayerLayoutIcon.setVisibility(View.VISIBLE);
            sportPlayerLayoutText.setText("");

            heartRateinfo.setVisibility(View.VISIBLE);
            scanProgressBar.setVisibility(View.GONE);
        } else {
            carPlayer.setEnabled(false);
            sportPlayer.setEnabled(false);

            carPlayerLayoutIcon.setVisibility(View.GONE);
//            carPlayerLayoutText.setText("Pripojte senzor");

            sportPlayerLayoutIcon.setVisibility(View.GONE);
//            sportPlayerLayoutText.setText("Pripojte senzor");

            heartRateinfo.setVisibility(View.GONE);
            scanProgressBar.setVisibility(View.VISIBLE);
        }

    }

    private void setUpListeners() {
        musicPlayerLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), MusicPlayer.class);
                startActivity(intent);
            }
        });
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

        carPlayerLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hrSenzorActive) {
                    Intent intent = new Intent(getApplicationContext(), CarPlayer.class);
                    startActivity(intent);
                }
            }
        });
        carPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), CarPlayer.class);
                startActivity(intent);
            }
        });

        sportPlayerLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hrSenzorActive) {
                    Intent intent = new Intent(getApplicationContext(), SportPlayer.class);
                    startActivity(intent);
                }
            }
        });
        sportPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SportPlayer.class);
                startActivity(intent);
            }
        });

        sleepPlayerLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SleepPlayer.class);
                startActivity(intent);
            }
        });
        sleepPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SleepPlayer.class);
                startActivity(intent);
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void provideDisconnectAction() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hrSenzorActive = false;
                inicializeUI();
                scanForDevice();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    void scanForDevice() {
        scanText.setText("Senzor pulzu nie je pripojený. Hľadám...");
        scanText.setTextColor(getApplicationContext().getResources().getColor(R.color.white));
        scanProgressBar.setVisibility(View.VISIBLE);

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

                                scanProgressBar.setVisibility(View.GONE);
                                scanText.setText("Nájdené zariadenie " + polarDeviceInfo.name);
                                scanText.setTextColor(getApplicationContext().getResources().getColor(R.color.green));


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
                provideDisconnectAction();
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

                        if (polarBroadcastData != null) {
                            heartRateinfo.setVisibility(View.VISIBLE);
                            heartRateinfo.setText(String.valueOf(polarBroadcastData.hr));
                            if (!hrSenzorActive) {
                                hrSenzorActive = true;
                                inicializeUI();
                            }
                        }


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
                        Log.d(TAG, "complete SenzorOFF");
                    }
                }
        );
    }

    public void inicializeTermination() {
        String value = getIntent().getStringExtra("TERMINATE_APP");
        if (value != null) {
            Log.e("TERMINATOR", getIntent().getStringExtra("TERMINATE_APP"));
            if (getIntent().getStringExtra("TERMINATE_APP").equals("1")) {
                finishAndRemoveTask();
                System.exit(0);
            }
        }
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted so we ask for it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    12);
        }
    }

    @SuppressLint("SetTextI18n")
    private void checkBluetoothAndGPS() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);


        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            scanText.setText("Povolte  GPS služby");
            scanText.setTextColor(getApplicationContext().getResources().getColor(R.color.red));

            scanText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(gpsOptionsIntent, REQUEST_ENABLE_GPS);
                }
            });
        } else checkForPermission();

//        scanForDevice();
    }


}
