package com.example.ks_talks;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import de.hdodenhof.circleimageview.CircleImageView;

public class registration extends AppCompatActivity {

    TextView loginbut;
    EditText re_username, rg_email, rg_password, rg_repassword;
    Button rg_signup;
    CircleImageView rg_profileImg;

    FirebaseAuth auth;
    FirebaseDatabase database;
    FirebaseStorage storage;

    Uri imageURI;
    String imageuri;

    ProgressDialog progressDialog;

    String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Firebase init
        auth = FirebaseAuth.getInstance();

        // ✅ AUTO LOGIN CHECK (MAIN FIX)
        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(registration.this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_registration);

        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating your account...");
        progressDialog.setCancelable(false);

        // ✅ Views
        loginbut = findViewById(R.id.loginbut);
        re_username = findViewById(R.id.rgusername);
        rg_email = findViewById(R.id.rgEmail);
        rg_password = findViewById(R.id.rgpassword);
        rg_repassword = findViewById(R.id.rgrepassword);
        rg_profileImg = findViewById(R.id.profilerg0);
        rg_signup = findViewById(R.id.signupbutton);

        // ✅ Go to Login Page
        loginbut.setOnClickListener(v -> {
            startActivity(new Intent(registration.this, login.class));
            finish();
        });

        // ✅ Image Picker
        rg_profileImg.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, 10);
        });

        // ✅ Signup Button
        rg_signup.setOnClickListener(v -> {

            String name = re_username.getText().toString().trim();
            String email = rg_email.getText().toString().trim();
            String password = rg_password.getText().toString().trim();
            String cpassword = rg_repassword.getText().toString().trim();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) ||
                    TextUtils.isEmpty(password) || TextUtils.isEmpty(cpassword)) {
                Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!email.matches(emailPattern)) {
                rg_email.setError("Invalid Email");
                return;
            }

            if (password.length() < 6) {
                rg_password.setError("Minimum 6 characters");
                return;
            }

            if (!password.equals(cpassword)) {
                rg_repassword.setError("Password not matched");
                return;
            }

            progressDialog.show();

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {

                            String uid = auth.getCurrentUser().getUid();
                            DatabaseReference ref = database.getReference("user").child(uid);
                            StorageReference store = storage.getReference("profile").child(uid);

                            if (imageURI != null) {
                                store.putFile(imageURI)
                                        .addOnSuccessListener(taskSnapshot ->
                                                store.getDownloadUrl().addOnSuccessListener(uri -> {

                                                    imageuri = uri.toString();
                                                    Users users = new Users(
                                                            uid,
                                                            name,
                                                            email,
                                                            "Hey I'm Using This App",
                                                            imageuri,
                                                            password
                                                    );

                                                    ref.setValue(users).addOnSuccessListener(unused -> {
                                                        progressDialog.dismiss();
                                                        startActivity(new Intent(registration.this, MainActivity.class));
                                                        finish();
                                                    });
                                                })
                                        );
                            } else {
                                Users users = new Users(
                                        uid,
                                        name,
                                        email,
                                        "Hey I'm Using This App",
                                        null,
                                        password
                                );

                                ref.setValue(users).addOnSuccessListener(unused -> {
                                    progressDialog.dismiss();
                                    startActivity(new Intent(registration.this, MainActivity.class));
                                    finish();
                                });
                            }
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10 && resultCode == RESULT_OK && data != null) {
            imageURI = data.getData();
            rg_profileImg.setImageURI(imageURI);
        }
    }
}
