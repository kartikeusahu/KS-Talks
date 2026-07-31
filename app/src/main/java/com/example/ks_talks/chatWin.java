package com.example.ks_talks;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class chatWin extends AppCompatActivity {

    String reciverimg, recivername, reciverUID, senderUID;
    CircleImageView profile;
    TextView reciverNName;
    ImageView sentbtn;
    EditText textmsg;

    FirebaseAuth firebaseAuth;
    FirebaseDatabase database;

    public static String senderImg;
    public static String reciverIImg;

    String senderRoom;   // ✅ only ONE room now

    RecyclerView mmssangerAdapter;
    ArrayList<msgModelclass> messagessArrayList;
    messagesAdpter messagesAdpter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_win);

        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        messagessArrayList = new ArrayList<>();

        mmssangerAdapter = findViewById(R.id.msgadpter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        mmssangerAdapter.setLayoutManager(linearLayoutManager);

        messagesAdpter = new messagesAdpter(chatWin.this, messagessArrayList);
        mmssangerAdapter.setAdapter(messagesAdpter);

        recivername = getIntent().getStringExtra("name");
        reciverimg = getIntent().getStringExtra("reciverImg");
        reciverUID = getIntent().getStringExtra("uid");

        sentbtn = findViewById(R.id.sendbtnn);
        textmsg = findViewById(R.id.textmsg);
        profile = findViewById(R.id.profilerg0);
        reciverNName = findViewById(R.id.reciverName);

        Picasso.get().load(reciverimg).into(profile);
        reciverNName.setText(recivername);

        senderUID = firebaseAuth.getUid();

        // ✅ ✅ FIXED ROOM LOGIC (MOST IMPORTANT)
        senderRoom = senderUID.compareTo(reciverUID) < 0
                ? senderUID + reciverUID
                : reciverUID + senderUID;

        // ✅ User profile ref
        DatabaseReference reference = database.getReference()
                .child("user")
                .child(senderUID);

        // ✅ SAME chat room for sender & receiver
        DatabaseReference chatrefrecnce = database.getReference()
                .child("chats")
                .child(senderRoom)
                .child("messages");

        // ✅ MESSAGE LISTENER (NOW WORKS FOR RECEIVER TOO)
        chatrefrecnce.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messagessArrayList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    msgModelclass messages = dataSnapshot.getValue(msgModelclass.class);
                    messagessArrayList.add(messages);
                }
                messagesAdpter.notifyDataSetChanged();
                mmssangerAdapter.scrollToPosition(messagessArrayList.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                senderImg = snapshot.child("profilepic").getValue(String.class);
                reciverIImg = reciverimg;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        // ✅ SEND MESSAGE (ONLY ONE PUSH)
        sentbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String message = textmsg.getText().toString().trim();
                if (message.isEmpty()) {
                    Toast.makeText(chatWin.this, "Please enter your message", Toast.LENGTH_SHORT).show();
                    return;
                }

                textmsg.setText("");

                msgModelclass messagess = new msgModelclass(
                        senderUID,
                        message,
                        System.currentTimeMillis()
                );

                database.getReference()
                        .child("chats")
                        .child(senderRoom)
                        .child("messages")
                        .push()
                        .setValue(messagess);
            }
        });
    }
}
