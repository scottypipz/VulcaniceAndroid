package com.vulcanice.vulcanice.ClientRequest;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.vulcanice.vulcanice.MainPage;
import com.vulcanice.vulcanice.Model.Request;
import com.vulcanice.vulcanice.Model.VCN_User;
import com.vulcanice.vulcanice.R;
import com.vulcanice.vulcanice.TrackRequestActivity;

/**
 * Created by User on 16/06/2018.
 */

public class RequestShopActivity extends AppCompatActivity{
    protected Context context;
    private FirebaseDatabase mDatabase;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference requestReference;

    protected Button btnRequest;
    protected TextView clientDescription, mShopName;
    protected Spinner vehicleType, pickupType;
    protected EditText plateNumber1, plateNumber2, vehicleColor, repairType;
    protected ProgressBar progressBar;
    protected Request request;
    protected VCN_User vcnUser;
    protected String shopId, shopName;
    protected CountDownTimer countDownTimer;

    protected TextView countdown;
    protected LinearLayout waitingRequest;
    protected LinearLayout processRequest;
    protected LinearLayout requestDeclined;
    protected LinearLayout requestTimeout;
    protected ConstraintLayout confirmRequest;

    private OnSuccessListener onRequestSuccess = new OnSuccessListener() {
        @Override
        public void onSuccess(Object o) {
            listenForRequest();
        }
    };

    private OnFailureListener onRequestFailed = new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception e) {
            Toast.makeText(
                    RequestShopActivity.this,
                    "Failed to request tracking\nPlease try again later",
                    Toast.LENGTH_SHORT
            ).show();
        }
    };

    private OnCompleteListener onRequestCompleted = new OnCompleteListener() {
        @Override
        public void onComplete(@NonNull Task task) {
            progressBar.setVisibility(View.GONE);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_shop);

        context = RequestShopActivity.this;
        setupView();
        setupFirebase();
        getUserInfo();
        setEvents();
    }

    private void setupView() {
        // ProgressBar
        progressBar = findViewById(R.id.progress_bar);
        // Button
        btnRequest = findViewById(R.id.btn_request);
        // TextView
        countdown = findViewById(R.id.countdown);
        mShopName = findViewById(R.id.txt_shop_name);
        // Spinner
        vehicleType = findViewById(R.id.vehicle_type);
        pickupType = findViewById(R.id.pickup_type);
        // EditText
        vehicleColor = findViewById(R.id.vehicle_color);
        plateNumber1 = findViewById(R.id.plate_number_1);
        plateNumber2 = findViewById(R.id.plate_number_2);
        repairType = findViewById(R.id.repair_type);
        clientDescription = findViewById(R.id.user_description);
        // LinearLayout
        waitingRequest = findViewById(R.id.waiting_request);
        processRequest = findViewById(R.id.process_request);
        requestDeclined = findViewById(R.id.request_declined);
        requestTimeout = findViewById(R.id.request_timeout);
        confirmRequest = findViewById(R.id.confirm_request);

        populateSpinners();

        Intent i = getIntent();
        shopId = i.getExtras().getString("shopId");
        shopName = i.getExtras().getString("shopName");

        shopId = shopId.split("_")[0];
        getSupportActionBar().setTitle(shopName);
    }

    private void listenForRequest() {

        waitingRequest.setVisibility(View.VISIBLE);
        processRequest.setVisibility(View.GONE);

        // TODO remove timer if has other action (if declined or cancelled)
        // SET TIME OUT FOR 60 SECONDS
        countDownTimer = new CountDownTimer(60000, 1000) {
            public void onTick(long millisUntilFinished) {
                handleCountdown();
            }

            public void onFinish() {
                handleRequestTimeout();
            }
        };
        countDownTimer.start();

        // CHECK IF THERE'S AN ACTION MADE
        DatabaseReference isAcceptedReference = requestReference.child("isAccepted");
        isAcceptedReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if ( ! dataSnapshot.exists()) {
                    return;
                }
                countDownTimer.cancel();
                if (isRequestDeclined(dataSnapshot)) {
                    handleRequestDeclined();
                    return;
                }
                if (isRequestAccepted(dataSnapshot)) {
                    handleRequestAccepted();
                    return;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                String dbError = RequestShopActivity.this.getString(R.string.db_error);
                Toast.makeText(context, dbError, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleRequestDeclined () {
        showUI(requestDeclined);
    }

    private void handleRequestAccepted () {
        final FloatingActionButton acceptConfirmation = findViewById(R.id.btn_accept_confirmation);
        final FloatingActionButton declineConfirmation = findViewById(R.id.btn_decline_confirmation);

        View.OnClickListener onClickAccept = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatabaseReference reqRef = mDatabase.getReference()
                        .child("Request").child(shopId).child(currentUser.getUid());

                reqRef.child("isAccepted").setValue(4).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if ( ! task.isSuccessful()) {
                            Toast.makeText(
                                    RequestShopActivity.this,
                                    "Oops.. Unable to process your request.\nPlease try again later.",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }
                        Intent intent = new Intent(RequestShopActivity.this, TrackRequestActivity.class);
                        intent.putExtra("id", shopId);
                        intent.putExtra("type", "client");
                        startActivity(intent);
                    }
                });
            }
        };
        View.OnClickListener onClickDecline = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeRequest();
                Intent i = new Intent(context, MainPage.class);
                startActivity(i);
                finish();
            }
        };
        acceptConfirmation.setOnClickListener(onClickAccept);
        declineConfirmation.setOnClickListener(onClickDecline);
        showUI(confirmRequest);
    }

    private void handleRequestTimeout () {
        removeRequest();
        showUI(requestTimeout);
    }

    private void handleCountdown () {
        int time = Integer.parseInt(countdown.getText().toString());
        Log.d("Timer_countdown", time + "");
        if (time > 0) {
            time --;
            countdown.setText(time + "");
        }
    }

    private void showUI (LinearLayout layout) {
        requestTimeout.setVisibility(View.GONE);
        waitingRequest.setVisibility(View.GONE);
        processRequest.setVisibility(View.GONE);

        layout.setVisibility(View.VISIBLE);
    }
    private void showUI (ConstraintLayout layout) {
        requestTimeout.setVisibility(View.GONE);
        waitingRequest.setVisibility(View.GONE);
        processRequest.setVisibility(View.GONE);

        layout.setVisibility(View.VISIBLE);
    }

    private boolean isRequestAccepted(DataSnapshot dataSnapshot) {
        return dataSnapshot.getValue().toString().equals("1");
    }

    private boolean isRequestDeclined(DataSnapshot dataSnapshot) {
        return dataSnapshot.getValue().toString().equals("2");
    }
    protected void populateSpinners() {
        // VEHICLE TYPE
        ArrayAdapter<CharSequence> vehicleTypeAdapter = ArrayAdapter.createFromResource(this,
                R.array.vcn_vehicle_type, android.R.layout.simple_list_item_activated_1);

        vehicleTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vehicleType.setAdapter(vehicleTypeAdapter);
        vehicleType.setSelection(0);

        // PICKUP TYPE
        ArrayAdapter<CharSequence> pickupTypeAdapter = ArrayAdapter.createFromResource(this,
                R.array.vcn_pickup_type, android.R.layout.simple_list_item_activated_1);
        vehicleTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pickupType.setAdapter(pickupTypeAdapter);
        pickupType.setSelection(0);
    }

    private void setEvents() {
        View.OnClickListener onClickRequest = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("is_logged_in", (vcnUser == null) + "");
                if (vcnUser == null) {
                    return;
                }
                handleSubmitRequestShop();
            }
        };

        btnRequest.setOnClickListener(onClickRequest);
    }
    private void handleSubmitRequestShop() {

        String nClientDescription = clientDescription.getText().toString().trim();
        if (nClientDescription.equals("")) {
            alertNoDescription();
            return;
        }
        request.setDescription(nClientDescription);
        request.setValid(true);
        request.setClientUid(currentUser.getUid());
        request.setIsAccepted(0);
        request.setVehicleType(vehicleType.getSelectedItem().toString());
        request.setVehicleColor(vehicleColor.getText().toString().trim());
        request.setPickupType(pickupType.getSelectedItem().toString());
        request.setRepairType(repairType.getText().toString().trim());
        request.setPlateNumber(plateNumber1.getText().toString().trim() + "-" + plateNumber2.getText().toString().trim());

        progressBar.setVisibility(View.VISIBLE);
        requestReference = mDatabase.getReference()
                .child("Request").child(shopId).child(currentUser.getUid());

        requestReference.setValue(request)
                .addOnSuccessListener(onRequestSuccess)
                .addOnFailureListener(onRequestFailed)
                .addOnCompleteListener(onRequestCompleted);
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance();
    }

    private void getUserInfo() {
        request = new Request();
        Log.d("current_user", currentUser.getUid());
        DatabaseReference shopReference = mDatabase.getReference()
                .child("Users").child(currentUser.getUid());

        shopReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                vcnUser = dataSnapshot.getValue(VCN_User.class);
                if (vcnUser == null) {
                    return;
                }
                request.setClientName(vcnUser.getName());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                String dbError = RequestShopActivity.this.getString(R.string.db_error);
                Toast.makeText(RequestShopActivity.this, dbError, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void alertNoDescription() {
        Toast.makeText(
                RequestShopActivity.this,
                "Please enter a description",
                Toast.LENGTH_SHORT
        ).show();
    }

    public void gotoMainPage() {
        startActivity(new Intent(RequestShopActivity.this, MainPage.class));
    }

    @Override
    protected void onStop() {
        removeRequest();
        super.onStop();
    }

    private void removeRequest() {
        DatabaseReference reqRef = mDatabase.getReference()
            .child("Request").child(shopId).child(currentUser.getUid());

        reqRef.child("isAccepted").setValue(3);
    }

    @Override
    public void onBackPressed() {
        removeRequest();
        startActivity(
                new Intent(RequestShopActivity.this, MainPage.class)
        );
        super.onStop();
    }

}
