package ke.co.kbanda.tafuta;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import ke.co.kbanda.tafuta.adapters.MembersListAdapter;
import ke.co.kbanda.tafuta.models.Member;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private RecyclerView recyclerView;
    private MembersListAdapter recyclerViewAdapter;
    private List<Member> membersList;
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressDialog = new ProgressDialog(this);
        membersList = new ArrayList<>();

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();

        recyclerView = findViewById(R.id.recyclerViewMembersList);
        recyclerViewAdapter = new MembersListAdapter(membersList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(recyclerViewAdapter);

        recyclerViewAdapter
                .setOnMemberClickedListener(new MembersListAdapter.OnMemberClickedListener() {
                    @Override
                    public void onClick(int position) {
                        Member member = membersList.get(position);

                        Intent intent = new Intent(MainActivity.this, TrackingActivity.class);
                        intent.putExtra("member", member);
                        startActivity(intent);
                    }
                })
        ;

        findViewById(R.id.fabAddMember)
                .setOnClickListener(v -> {
                    startActivity(new Intent(this, AddMemberActivity.class));
                })
        ;

        fetchDataFromDatabase();
    }

    private void fetchDataFromDatabase() {
        progressDialog.setMessage("Loading...");
        progressDialog.show();
        databaseReference
                .child("Members")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        progressDialog.dismiss();
                        membersList.clear();
                        for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                            Member member = memberSnapshot.getValue(Member.class);
                            Log.d(TAG, "onDataChange: New Member -> " + member);
                            membersList.add(member);
                            recyclerViewAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressDialog.dismiss();
                        Log.d(TAG, "onCancelled: Fetch cancelled -> " + error.getMessage());
                    }
                })
        ;
    }
}
