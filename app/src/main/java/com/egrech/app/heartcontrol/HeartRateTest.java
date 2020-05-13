package com.egrech.app.heartcontrol;

import androidx.appcompat.app.AppCompatActivity;
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

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.UUID;

public class HeartRateTest extends AppCompatActivity {

    private TextView hrTestTitle;
    private Button hrShortTestButton;
    private Button hrLongTestButton;

    private ImageView hrTestImage;
    private TextView calmQuestionText;
    private TextView durationText;
    private TextView testInfo;

    private Button startButton;

    private int averageHRText;
    private User currentUser;

    private int durationMS;
    private CountDownTimer countDown;
    boolean timeRunning = false;
    private ProgressBar circleSpin;
    private ProgressBar scanSpin;


    int heartRate;
    PolarBleApi api;
    Disposable broadcastDisposable;
    Disposable scanDisposable;

    String DEVICE_ID = "";
//    String DEVICE_ID = "612F262A";
    int senzorData = -1;


    public static final String TAG = "HeartRateTestingScreen";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_rate_test);


        getCurrentUser();
        initializeUI();
        setListeners();

        scanForDevice();
//        connectPolarDevice();
    }

    private void initializeUI() {
        hrLongTestButton = (Button) findViewById(R.id.hr_test_long_button);
        hrShortTestButton = (Button) findViewById(R.id.hr_test_short_button);
        hrTestTitle = (TextView) findViewById(R.id.hr_test_title);

        hrTestImage = (ImageView) findViewById(R.id.hr_test_image);
        hrTestImage.setVisibility(View.GONE);

        calmQuestionText = (TextView) findViewById(R.id.hr_test_calm_text);
        calmQuestionText.setVisibility(View.GONE);

        durationText = (TextView) findViewById(R.id.hr_test_duration_text);
        durationText.setVisibility(View.GONE);

        circleSpin = (ProgressBar) findViewById(R.id.hr_test_progressBar);
        circleSpin.setVisibility(View.GONE);

        scanSpin = (ProgressBar) findViewById(R.id.hr_test_scan_spin);
        scanSpin.setVisibility(View.GONE);

        startButton = (Button) findViewById(R.id.hr_test_start_button);
        startButton.setVisibility(View.GONE);

        testInfo = (TextView) findViewById(R.id.hr_test_test_info);

    }

    private void setListeners() {
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hrTestTitle.setVisibility(View.GONE);
                startHRMeasurmentTest();
            }
        });

        hrShortTestButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                durationMS = 180000;
                hrTestTitle.setText("Dĺžka testu 3 minúty");
                testInfo.setText(getApplicationContext().getResources().getText(R.string.short_test_info));
                startButton.setVisibility(View.VISIBLE);
            }
        });
        hrLongTestButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                durationMS = 300000;
                hrTestTitle.setText("Dĺžka testu 5 minút");
                testInfo.setText(getApplicationContext().getResources().getText(R.string.long_test_info));
                startButton.setVisibility(View.VISIBLE);
            }
        });
    }

    private void startHRMeasurmentTest() {
        testInfo.setVisibility(View.GONE);
        hrTestTitle.setVisibility(View.GONE);
        hrTestImage.setVisibility(View.VISIBLE);
        calmQuestionText.setVisibility(View.VISIBLE);
        durationText.setVisibility(View.VISIBLE);
        circleSpin.setVisibility(View.VISIBLE);
        hrShortTestButton.setVisibility(View.GONE);
        hrLongTestButton.setVisibility(View.GONE);

        startTimer();
    }

    void getCurrentUser() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference dbUsersRef = database.getReference("users");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        assert user != null;
        dbUsersRef.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User value = dataSnapshot.getValue(User.class);
                Log.d("GetUser", "Value is: " + value);
                currentUser = value;
                if (value != null) {
                    averageHRText = value.averageHeartRate;
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.d("LoginUserCheck", "Didnt catch data of user");
            }
        });
    }


    @SuppressLint("SetTextI18n")
    private void startTimer() {
        durationText.setVisibility(View.VISIBLE);


        countDown = new CountDownTimer(durationMS, 1000) {
            @SuppressLint("SetTextI18n")
            @Override
            public void onTick(long millisUntilFinished) {
                durationMS = (int) millisUntilFinished;

                int hours = Math.floorDiv((int) durationMS, 3600000);
                int minutes = Math.floorDiv((int) durationMS % 3600000, 60000);
                int secondes = (int) ((durationMS % 36000000) % 60000) / 1000;

                durationText.setText("0" + minutes + ":" + (secondes < 10 ? "0" + secondes : secondes));
//                Log.e("COUNTDOWN", String.valueOf(hours) + ":" + String.valueOf(minutes) + ":" + String.valueOf(secondes));
                timeRunning = true;
            }

            @Override
            public void onFinish() {
//                writeTimeStamp();
            }
        }.start();
    }


    // tento blok kodu kazdych 7 sekund spravi priemer zo ziskanych hodnot zo senzoru a stanovi priemernu hodnotu tepu
    ArrayList<Integer> hrValues = new ArrayList<Integer>();

    public static Handler myHandler = new Handler();
    private static final int TIME_TO_WAIT = 7000; //

    Runnable myRunnable = new Runnable() {
        @Override
        public void run() {

            int hrCount = 0;
            for (int i = 0; i < hrValues.size(); i++) {
                hrCount += hrValues.get(i);
            }

            heartRate = hrCount / hrValues.size();

            Log.e(TAG, "" + heartRate);
//            if (heartRate < sleepTreshold) {
//                if(currentUser.sleepAverageValues != null) {
//                    currentUser.sleepAverageValues = currentUser.sleepAverageValues + " " + heartRate;
//                } else currentUser.sleepAverageValues = String.valueOf(heartRate);
//                saveUser(currentUser.getUserId());
//                writeTimeStamp();
//            } else {
//                hrValues.clear();
//                restartCountingHR();
//            }

            //Log.e("HEARTRATE", "Actual avaregae measurment of Heart Rate is: "+heartRate);
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


    @SuppressLint("SetTextI18n")
    void scanForDevice() {
        hrTestTitle.setText("Scanning for device");
        hrShortTestButton.setVisibility(View.GONE);
        hrLongTestButton.setVisibility(View.GONE);
        scanSpin.setVisibility(View.VISIBLE);

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

                            if(deviceNameMark.equals("Polar")) {
                                DEVICE_ID = polarDeviceInfo.address;

                                scanDisposable.dispose();

                                hrTestTitle.setText("Vyberte si dĺžku testu");
                                hrShortTestButton.setVisibility(View.VISIBLE);
                                hrLongTestButton.setVisibility(View.VISIBLE);
                                scanSpin.setVisibility(View.GONE);

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

                        senzorData = polarBroadcastData.hr;


                        hrValues.add(senzorData);


                        if (!runnableFlag[0]) {
                            startCountingHR(polarBroadcastData.hr);
                            runnableFlag[0] = true; // when we started average counting, we can disable start of runnable counter
                        }

                        Log.d(TAG, "HR SenZOR OH1 BROADCAST " +
                                polarBroadcastData.polarDeviceInfo.deviceId + " HR: " +
                                polarBroadcastData.hr + " batt: " +
                                polarBroadcastData.batteryStatus);
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
