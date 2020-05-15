package com.egrech.app.heartcontrol;
import android.content.Intent;
import android.os.Bundle;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

public class UserProfile extends AppCompatActivity {

    EditText name;
    EditText age;
    Spinner gender_spinner, sport_spinner, smoking_spinner, work_spinner, patology_spinner;
    User currentUser;
    Button save_data;
    private FirebaseAuth mAuth;
    private FirebaseDatabase mFirebaseDatabaseInstance;
    private DatabaseReference mFirebaseDatabase;

    private ImageView averageHRIcon;
    private TextView averageHRText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        initializeUI();         //inicializuj User Interface
        getCurrentUser();       //stiahni data aktualneho usera z databazy
        setUpListeners();       //nastav listeneri na zmenu spinnerov

    }

    @Override
    protected void onResume() {
        super.onResume();
        getCurrentUser();
        setUpListeners();
    }

    void initializeUI() {
        // nastav layout views

        gender_spinner = (Spinner) findViewById(R.id.profileGender);
        sport_spinner = (Spinner) findViewById(R.id.profileSportActivity);
        smoking_spinner = (Spinner) findViewById(R.id.profileSmoker);
        work_spinner = (Spinner) findViewById(R.id.profileWorkActivity);
        patology_spinner = (Spinner) findViewById(R.id.profilePatology);

        save_data = (Button) findViewById(R.id.profile_save_data);

        age = (EditText) findViewById(R.id.profileAge);
        name = (EditText) findViewById(R.id.profileName);
        averageHRIcon = (ImageView) findViewById(R.id.profile_average_hr_heart_icon);
        averageHRText = (TextView) findViewById(R.id.profile_average_hr_text);


        averageHRIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), HeartRateTest.class);
                startActivity(intent);
            }
        });

        averageHRText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), HeartRateTest.class);
                startActivity(intent);
            }
        });

        save_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUser(currentUser);
                save_data.setVisibility(View.GONE);
            }
        });
    }

    void getCurrentUser() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference dbUsersRef = database.getReference("users");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        dbUsersRef.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                User value = dataSnapshot.getValue(User.class);

                Log.d("GetUser", "Value is: " + value);
                currentUser = value;

                if(value != null) {
                    averageHRText.setText(String.valueOf(value.averageHeartRate));
                    setViews(value);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.d("LoginUserCheck", "Didnt catch data of user");
            }
        });
    }

    private void saveUser(User currentUser) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference dbUsersRef = database.getReference("users");
        dbUsersRef.child(String.valueOf(currentUser.userId)).setValue(currentUser);
    }

    private void setUpListeners() {
        gender_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (currentUser != null) {
                    save_data.setVisibility(View.VISIBLE);
                    currentUser.gender = position;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        sport_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (currentUser != null) {
                    save_data.setVisibility(View.VISIBLE);
                    currentUser.sporting = String.valueOf(currentUser.setSportingLevel(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        smoking_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (currentUser != null) {
                    save_data.setVisibility(View.VISIBLE);
                    currentUser.smoker = position;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        work_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (currentUser != null) {
                    save_data.setVisibility(View.VISIBLE);
                    currentUser.workingActivity = position;
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        patology_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (currentUser != null) {
                    save_data.setVisibility(View.VISIBLE);
                    currentUser.patology = position;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        age.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                save_data.setVisibility(View.VISIBLE);
                currentUser.age = Integer.parseInt(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                save_data.setVisibility(View.VISIBLE);
                currentUser.userName = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setViews(User currentUser) {

        name = (EditText) findViewById(R.id.profileName);
        age = (EditText) findViewById(R.id.profileAge);

        name.setText(String.valueOf(currentUser.userName));
        age.setText(String.valueOf(currentUser.age));

        ArrayAdapter<CharSequence> gender_adapter = ArrayAdapter.createFromResource(this,
                R.array.gender_values, android.R.layout.simple_spinner_item);
        gender_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gender_spinner.setAdapter(gender_adapter);
        gender_spinner.setSelection(currentUser.gender);

        ArrayAdapter<CharSequence> sportActivity_adapter = ArrayAdapter.createFromResource(this,
                R.array.sport_activity_values, android.R.layout.simple_spinner_item);
        sportActivity_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sport_spinner.setAdapter(sportActivity_adapter);
        int sportSpinnerPosition = currentUser.getSportingLevel(currentUser.sporting);         // getSportingLevel je funkcia v class user ktora stringovu enum hodnotu ziskanu z databazy premeni na poziciu v spinneri
        sport_spinner.setSelection(sportSpinnerPosition);

        ArrayAdapter<CharSequence> smoking_adapter = ArrayAdapter.createFromResource(this,
                R.array.smoking_values, android.R.layout.simple_spinner_item);
        smoking_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        smoking_spinner.setAdapter(smoking_adapter);
        int smokingSpinnerPosition = smoking_adapter.getPosition(String.valueOf(currentUser.smoker));
        smoking_spinner.setSelection(smokingSpinnerPosition);
        smoking_spinner.setSelection(currentUser.smoker);

        ArrayAdapter<CharSequence> workActivity_adapter = ArrayAdapter.createFromResource(this,
                R.array.work_activity_values, android.R.layout.simple_spinner_item);
        workActivity_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        work_spinner.setAdapter(workActivity_adapter);
        int workSpinnerPosition = workActivity_adapter.getPosition(String.valueOf(currentUser.workingActivity));
        work_spinner.setSelection(workSpinnerPosition);
        work_spinner.setSelection(currentUser.workingActivity);

        ArrayAdapter<CharSequence> patology_adapter = ArrayAdapter.createFromResource(this,
                R.array.patology_values, android.R.layout.simple_spinner_item);
        patology_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        patology_spinner.setAdapter(patology_adapter);
        patology_spinner.setAdapter(patology_adapter);
        int patologySpinnerPosition = patology_adapter.getPosition(String.valueOf(currentUser.patology));
        patology_spinner.setSelection(patologySpinnerPosition);
        patology_spinner.setSelection(currentUser.patology);


        return;
    }

}
