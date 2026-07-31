package com.example.ks_talks;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

public class setting extends AppCompatActivity {

    ImageView setprofile;
    EditText setname, setstatus;
    Button donebut;

    FirebaseAuth auth;
    FirebaseDatabase database;
    FirebaseStorage storage;
    DatabaseReference reference;
    StorageReference storageReference;

    Uri setImageUri;

    String email = "";
    String password = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        auth = FirebaseAuth.getInstance();

        // ✅ LOGIN CHECK
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        setprofile = findViewById(R.id.settingprofile);
        setname = findViewById(R.id.settingname);
        setstatus = findViewById(R.id.settingstatus);
        donebut = findViewById(R.id.donebut);

        reference = database.getReference("user").child(auth.getUid());
        storageReference = storage.getReference("upload").child(auth.getUid());

        // ✅ SAFE DATA LOAD
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) return;

                email = snapshot.child("mail").getValue(String.class);
                password = snapshot.child("password").getValue(String.class);
                String name = snapshot.child("username").getValue(String.class);
                String profile = snapshot.child("profilepic").getValue(String.class);
                String status = snapshot.child("status").getValue(String.class);

                if (name != null) setname.setText(name);
                if (status != null) setstatus.setText(status);

                if (profile != null && !profile.isEmpty()) {
                    Picasso.get()
                            .load(profile)
                            .into(setprofile);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(setting.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // ✅ IMAGE PICK
        setprofile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, 10);
        });

        // ✅ SAVE BUTTON
        donebut.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {

        String name = setname.getText().toString().trim();
        String status = setstatus.getText().toString().trim();

        if (name.isEmpty()) {
            setname.setError("Enter name");
            return;
        }

        if (setImageUri != null) {
            storageReference.putFile(setImageUri)
                    .addOnSuccessListener(task ->
                            storageReference.getDownloadUrl()
                                    .addOnSuccessListener(uri ->
                                            saveData(uri.toString(), name, status)
                                    )
                    );
        } else {
            reference.child("profilepic").get()
                    .addOnSuccessListener(snap ->
                            saveData(snap.getValue(String.class), name, status)
                    );
        }
    }

    private void saveData(String img, String name, String status) {

        Users users = new Users(
                auth.getUid(),
                name,
                email,
                password,
                img,
                status
        );

        reference.setValue(users)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10 && resultCode == RESULT_OK && data != null) {
            setImageUri = data.getData();
            setprofile.setImageURI(setImageUri);
        }
    }
}
