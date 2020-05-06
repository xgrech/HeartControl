package com.egrech.app.heartcontrol;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SetNewUser extends AppCompatActivity {

    Button ready_button;
    ImageView question_image;
    TextView question;
    Spinner gender_spinner, sport_spinner, smoking_spinner, work_spinner, patology_spinner;
    EditText age;
    User newUser;
    int question_counter = 0;

    private FirebaseAuth mAuth;
    private FirebaseDatabase mFirebaseDatabaseInstance;
    private DatabaseReference mFirebaseDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_new_user);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        assert user != null;
        newUser = new User(String.valueOf(user.getUid()));
        newUser.userName = user.getDisplayName();

        question = (TextView) findViewById(R.id.set_up_question);
        question_image = (ImageView) findViewById(R.id.set_up_question_image);

        age = (EditText) findViewById(R.id.set_up_age_field);
        age.setVisibility(View.GONE);

        ready_button = (Button) findViewById(R.id.set_up_ready_button);

        gender_spinner = (Spinner) findViewById(R.id.set_up_gender_spinner);
        sport_spinner = (Spinner) findViewById(R.id.set_up_sport_spinner);
        smoking_spinner = (Spinner) findViewById(R.id.set_up_smoking_spinner);
        work_spinner = (Spinner) findViewById(R.id.set_up_work_spinner);
        patology_spinner = (Spinner) findViewById(R.id.set_up_patology_spinner);


        age.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                newUser.age = Integer.parseInt(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ready_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (question_counter < 6) {
                    ready_button.setText(R.string.next);
                    question_counter++;

                    question.setText(getQuestion(question_counter));
                    question_image.setVisibility(View.VISIBLE);
                    question_image.setImageResource(getQuestionImage(question_counter));


                    if (question_counter == 1) {
                        age.setVisibility(View.VISIBLE);
                    } else {
                        age.setVisibility(View.GONE);
                        updateSpinner(question_counter);
                    }

                } else {
                    saveUser(newUser, user.getUid());
                    Intent returnIntent = new Intent();
                    setResult(SetNewUser.RESULT_CANCELED, returnIntent);
                    finish();
                }
            }
        });


        gender_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                newUser.gender = position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        sport_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                newUser.sporting = String.valueOf(newUser.setSportingLevel(position));
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        smoking_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                newUser.smoker = position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        work_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                newUser.workingActivity = position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        patology_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                newUser.patology = position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

    }

    private void updateSpinner(int counter) {
        if (counter == 2) {
            ArrayAdapter<CharSequence> gender_adapter = ArrayAdapter.createFromResource(this, R.array.gender_values
                    , android.R.layout.simple_spinner_item);
            gender_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            gender_spinner.setAdapter(gender_adapter);
            gender_spinner.setVisibility(View.VISIBLE);
        }
        if (counter == 3) {
            gender_spinner.setVisibility(View.GONE);

            ArrayAdapter<CharSequence> sport_activity_adapter = ArrayAdapter.createFromResource(this, R.array.sport_activity_values
                    , android.R.layout.simple_spinner_item);
            sport_activity_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sport_spinner.setAdapter(sport_activity_adapter);
            sport_spinner.setVisibility(View.VISIBLE);
        }
        if (counter == 4) {
            sport_spinner.setVisibility(View.GONE);

            ArrayAdapter<CharSequence> smoking_adapter = ArrayAdapter.createFromResource(this, R.array.smoking_values
                    , android.R.layout.simple_spinner_item);
            smoking_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            smoking_spinner.setAdapter(smoking_adapter);
            smoking_spinner.setVisibility(View.VISIBLE);
        }
        if (counter == 5) {
            smoking_spinner.setVisibility(View.GONE);

            ArrayAdapter<CharSequence> work_activity_adapter = ArrayAdapter.createFromResource(this, R.array.work_activity_values
                    , android.R.layout.simple_spinner_item);
            work_activity_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            work_spinner.setAdapter(work_activity_adapter);
            work_spinner.setVisibility(View.VISIBLE);
        }
        if (counter == 6) {
            work_spinner.setVisibility(View.GONE);

            ArrayAdapter<CharSequence> patology_adapter = ArrayAdapter.createFromResource(this, R.array.patology_values
                    , android.R.layout.simple_spinner_item);
            patology_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            patology_spinner.setAdapter(patology_adapter);
            patology_spinner.setVisibility(View.VISIBLE);
        }
    }

    private int getQuestion(int counter) {
        switch (counter) {
            case 1:
                return R.string.age_question;
            case 2:
                return R.string.gender_question;
            case 3:
                return R.string.sport_question;
            case 4:
                return R.string.smoking_question;
            case 5:
                return R.string.working_question;
            case 6:
                return R.string.patology_question;
        }
        return 0;
    }

    private int getQuestionImage(int counter) {
        switch (counter) {
            case 1:
                return R.drawable.aging_icon;
            case 2:
                return R.drawable.gender_icon;
            case 3:
                return R.drawable.sporting_icon;
            case 4:
                return R.drawable.smoker_icon;
            case 5:
                return R.drawable.working_icon;
            case 6:
                return R.drawable.patology_icon;
        }
        return 0;
    }

//    private void getAverageHR () {
//        int averageHR = 90;
//        if(newUser.sporting == "PROFISPORTING") {
//            if(newUser.patology == 1){
//                if(newUser.smoker == 1) {
//                    averageHR = 65;
//                } else {
//                    averageHR = 60;
//                }
//            } else {
//                if(newUser.smoker != 1) {
//                    averageHR = 65;
//                } else {
//                    averageHR = 70;
//                }
//            }
//        } else if(newUser.sporting == "SPORTING") {
//            if(newUser.patology == 1){
//                if(newUser.smoker != 1) {
//                    averageHR = 70;
//                } else {
//                    averageHR = 75;
//                }
//            } else {
//                if(newUser.smoker != 1) {
//                    averageHR = 75;
//                } else {
//                    averageHR = 80;
//                }
//            }
//        } else if(newUser.sporting == "NOSPORT") {
//            if(newUser.workingActivity == 1){
//                if(newUser.patology != 1){
//                    if(newUser.smoker != 1) {
//                        averageHR = 70;
//                    } else {
//                        averageHR = 75;
//                    }
//                }else {
//                    if(newUser.smoker != 1) {
//                        averageHR = 80;
//                    } else {
//                        averageHR = 85;
//                    }
//                }
//            } else {
//                if(newUser.patology == 1){
//                    if(newUser.smoker != 1) {
//                        averageHR = 85;
//                    } else {
//                        averageHR = 90;
//                    }
//                }else {
//                    if(newUser.smoker != 1) {
//                        averageHR = 90;
//                    } else {
//                        averageHR = 95;
//                    }
//                }
//            }
//
//
//        }
//    }

    private void saveUser(User newUser, String userId) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference dbUsersRef = database.getReference("users");

        dbUsersRef.child(String.valueOf(userId)).setValue(newUser);
    }
}
