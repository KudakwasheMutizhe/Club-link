package com.example.course_link;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    ArrayList<ClubModal> clubModalArrayList = new ArrayList<>();
    ClubAdabter clubAdabter;
    SearchView searchView;

    LinearLayoutManager linearLayoutManager;

    // Firebase components for real-time database interaction
    private DatabaseReference databaseReference;
    private FirebaseDatabase firebaseDatabase;

    private static final String TAG = "MainActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Database instance and reference to "CourseInfo" node
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("ClubInfo");

        recyclerView = findViewById(R.id.clubRV);
        searchView = findViewById(R.id.searchView);

        searchView.clearFocus();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterList(newText);
                return true;
            }
        });

        // Initialize the ArrayList that will hold the course data retrieved from Firebase
         clubModalArrayList = new ArrayList<>();

        clubAdabter = new ClubAdabter(this, clubModalArrayList);
        recyclerView.setAdapter(clubAdabter);

        linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);

        // Fetch course data from Firebase and populate the RecyclerView
        fetchClubsFromFirebase();

    }

    private void filterList(String newText) {
        ArrayList<ClubModal> filteredList = new ArrayList<>();
        for (ClubModal clubModal : clubModalArrayList){
            if(clubModal.getClubName().toLowerCase().contains(newText.toLowerCase()) || clubModal.getShortDescription().toLowerCase().contains(newText.toLowerCase())){
                filteredList.add(clubModal);
            }
        }

        if (clubAdabter != null) {
            clubAdabter.setFilteredList(filteredList);
        }

        if(filteredList.isEmpty()){
            Toast.makeText(this, "No data found", Toast.LENGTH_SHORT).show();
        }else {
            clubAdabter.setFilteredList(filteredList);
        }
    }

    /**
     * Method to fetch course data from Firebase and update the UI.
     * It uses a ValueEventListener to listen for any changes in the "CourseInfo" node.
     */
    private void fetchClubsFromFirebase() {
        // Adding a ValueEventListener to listen for changes in the Firebase "CourseInfo" node

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Clear the existing data in the list before adding the new data
                clubModalArrayList.clear();

                // Loop through all data snapshots (courses) in the Firebase node
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    ClubModal clubModal = dataSnapshot.getValue(ClubModal.class);

                    // If the object is not null, add it to the course list
                    if (clubModal != null) {
                        Log.d(TAG, "Loaded club: id=" + clubModal.getClubId() + " name=" + clubModal.getClubName() + " imagePath=" + clubModal.getImagePath());
                        clubModalArrayList.add(clubModal);
                    }
                }

                // Notify the adapter that the data has been updated
                clubAdabter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle the error if the Firebase operation is cancelled or fails
                Toast.makeText(MainActivity.this, "Failed to load data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}