package com.egrech.app.heartcontrol;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;


public class Login extends AppCompatActivity {

    private static final int RC_SIGN_IN = 355;

    private FirebaseAuth mAuth;
    private FirebaseDatabase mFirebaseDatabaseInstance;
    private DatabaseReference mFirebaseDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build());
//                new AuthUI.IdpConfig.GoogleBuilder().build());
//                new AuthUI.IdpConfig.FacebookBuilder().build(),
//                new AuthUI.IdpConfig.TwitterBuilder().build());

        // Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setLogo(R.drawable.default_icon)
//                        .setIsSmartLockEnabled(false)


                        .build(),
                RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                // ...
                Log.e("SignIn", "User is " + user);



                checkExistingUser(user);

            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
                Log.e("SIgN IN","Sign in proccessss failedd.");
            }
        }
    }

    private void checkExistingUser(FirebaseUser user) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("users");

        String userId = user.getUid();

        boolean userFlag;
        // Read from the database
        myRef.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                User value = dataSnapshot.getValue(User.class);

                Log.d("LoginUserCheck", "Value is: " + value);

                if(value == null) {
                    Log.e("LoginUserCheck","This is New User");

                    User newUser = new User(userId);
                    myRef.child(user.getUid()).setValue(newUser);

                    Intent intent = new Intent(getApplicationContext(), Menu.class);
                    intent.putExtra("userId", userId);
                    intent.putExtra("newUserFlag", "true");
                    startActivity(intent);
                } else {
                    Log.e("LoginUserCheck","Not a New User");

                    Intent intent = new Intent(getApplicationContext(), Menu.class);
                    intent.putExtra("userId", userId);
                    intent.putExtra("newUserFlag", "false");
                    startActivity(intent);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.d("LoginUserCheck", "Didnt catch data");
                Log.e("LoginUserCheck","This is New User");


                User newUser = new User(userId);
                myRef.child(user.getUid()).setValue(newUser);

                Intent intent = new Intent(getApplicationContext(), Menu.class);
                intent.putExtra("userId", userId);
                intent.putExtra("newUserFlag", "true");
                startActivity(intent);
            }
        });
    }
}
