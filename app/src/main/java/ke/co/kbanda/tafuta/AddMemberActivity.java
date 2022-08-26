package ke.co.kbanda.tafuta;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.fxn.pix.Options;
import com.fxn.pix.Pix;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;
import ke.co.kbanda.tafuta.models.Member;

public class AddMemberActivity extends AppCompatActivity {
    private static final String TAG = "AddMemberActivity";
    public static final int GET_IMAGE_REQUESTCODE = 234;
    private CircleImageView memberImage;
    private MaterialToolbar toolbar;
    private TextInputLayout labelTIL;
    private TextInputLayout lastNameTIL;
    private TextInputLayout firstNameTIL;
    private TextInputLayout emailTIL;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseStorage firebaseStorage;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private ProgressDialog progressDialog;
    private String imageUri;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_member);

        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();
        firebaseStorage = FirebaseStorage.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        memberImage = findViewById(R.id.memberImage);
        emailTIL = findViewById(R.id.memberEmail);
        firstNameTIL = findViewById(R.id.memberFirstName);
        lastNameTIL = findViewById(R.id.memberLastName);
        labelTIL = findViewById(R.id.memberLabel);

        findViewById(R.id.submitMemberDetailsButton)
                .setOnClickListener(v -> {
                    saveMemberDetails();
                })
        ;


        findViewById(R.id.fabAddMemberProfileImage)
                .setOnClickListener(v -> {
                    Pix
                            .start(AddMemberActivity.this,
                                    Options
                                            .init()
                                            .setRequestCode(GET_IMAGE_REQUESTCODE)
                                            .setSpanCount(4)
                                            .setCount(1)
                                            .setMode(Options.Mode.Picture)
                                            .setPath(getString(R.string.app_name) + "/"));
                })
        ;
    }

    private void saveMemberDetails() {
        Log.d(TAG, "saveMemberDetails: ");
        String firstName = this.firstNameTIL.getEditText().getText().toString();
        String lastName = this.lastNameTIL.getEditText().getText().toString();
        String email = this.emailTIL.getEditText().getText().toString();
        String label = this.labelTIL.getEditText().getText().toString();

        if (!firstName.trim().isEmpty()) {
            firstNameTIL.setError(null);
            if (isEmailValid(email)) {
                emailTIL.setError(null);
                Member member = new Member(firstName, lastName, email, "", "", label);
                Log.d(TAG, "saveMemberDetails: Member object created -> " + member);
                if (isNetworkAvailable()) {
                    createMemberAccount(member);
                } else {
                    new AlertDialog
                            .Builder(this)
                            .setTitle("No Internet")
                            .setMessage("Please ensure that your device is connected to the internet before you continue!")
                            .create()
                            .show()
                    ;
                }
            } else {
                emailTIL.setError("Please enter a valid email address!");
                emailTIL.requestFocus();
            }
        } else {
            firstNameTIL.setError("First name is required");
            firstNameTIL.requestFocus();
        }
    }

    private void createMemberAccount(Member member) {
        if (member != null) {
            if (member.getEmail() != null && !member.getEmail().isEmpty()) {
                progressDialog.setMessage("Creating user account..!");
                progressDialog.show();
                String email = member.getEmail();

                // First Name will be the default password
                firebaseAuth
                        .createUserWithEmailAndPassword(email, email)
                        .addOnCompleteListener(task -> {
                            progressDialog.dismiss();
                            if (task.isSuccessful()) {
                                Log.d(TAG, "createMemberAccount: User Account created successfully");
                                if (task.getResult().getUser() != null) {
                                    String uid = task.getResult().getUser().getUid();//Get the user Id
                                    saveToDatabase(member, uid);
                                }
                            } else {
                                Toast.makeText(this, "Failed to create account: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "createMemberAccount: Failed to create User Account! -> " + task.getException().getMessage());
                            }
                        })
                ;
            } else {
                Log.d(TAG, "createMemberAccount: Member email is NULL or empty");
            }
        } else Log.d(TAG, "createMemberAccount: Member Object is NULL");
    }

    private boolean isEmailValid(String email) {
        return !email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void saveToDatabase(Member member, String userId) {
        Log.d(TAG, "saveToDatabase: ");
        if (member != null) {
            if (member.getEmail() != null && !member.getEmail().trim().isEmpty()) {
                progressDialog.setMessage("Saving details...");
                progressDialog.show();

                if (imageUri != null && !imageUri.trim().isEmpty()) {
                    Uri uri = Uri.fromFile(new File(imageUri));
                    StorageReference reference = FirebaseStorage.getInstance().getReference()
                            .child("Member")
                            .child(String.valueOf(System.currentTimeMillis()));
                    UploadTask uploadTask = reference.putFile(uri);
                    Task<Uri> uriTask = uploadTask
                            .continueWithTask(task -> {
                                if (!task.isSuccessful()) {
                                    throw task.getException();
                                }
                                return reference.getDownloadUrl();
                            })
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "saveToDatabase: Image saved successfully to Firebase Storage");
                                    if (task.getResult() != null) {
                                        final String url = task.getResult().toString();
                                        member.setImageUrl(url);

                                        databaseReference
                                                .child("Members")
                                                .child(userId)
                                                .setValue(member)
                                                .addOnCompleteListener(databaseTask -> {
                                                    progressDialog.dismiss();
                                                    if (databaseTask.isSuccessful()) {
                                                        Toast.makeText(this, "Member added successfully", Toast.LENGTH_SHORT).show();
                                                        finish();
                                                    } else {
                                                        Toast.makeText(this, "Failed to add member!", Toast.LENGTH_SHORT).show();
                                                        Log.d(TAG, "saveToDatabase: Failed to add member -> " + databaseTask.getException().getMessage());
                                                    }
                                                })
                                        ;
                                    }
                                } else {
                                    Log.d(TAG, "saveToDatabase: Failed to save image");
                                }
                            });
                } else {
                    databaseReference
                            .child("Members")
                            .child(userId)
                            .setValue(member)
                            .addOnCompleteListener(task -> {
                                progressDialog.dismiss();
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "Member added successfully", Toast.LENGTH_SHORT).show();
                                    finish();
                                } else {
                                    Toast.makeText(this, "Failed to add member!", Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "saveToDatabase: Failed to add member -> " + task.getException().getMessage());
                                }
                            })
                    ;
                }
            }
        } else {
            Log.d(TAG, "saveToDatabase: Member value is NULL!");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == GET_IMAGE_REQUESTCODE) {
            assert data != null;
            ArrayList<String> returnValue = data.getStringArrayListExtra(Pix.IMAGE_RESULTS);
            imageUri = returnValue.get(0);
            Glide
                    .with(this)
                    .load(returnValue.get(0))
                    .centerCrop()
                    .into(memberImage);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        @SuppressLint("MissingPermission") NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()) {
            // Network is present and connected
            isAvailable = true;
        }
        return isAvailable;
    }
}
