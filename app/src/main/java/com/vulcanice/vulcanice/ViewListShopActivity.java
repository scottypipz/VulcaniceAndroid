package com.vulcanice.vulcanice;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.webkit.GeolocationPermissions;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.vulcanice.vulcanice.Model.Shop;
import com.vulcanice.vulcanice.Model.ShopList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewListShopActivity extends AppCompatActivity {
    private FirebaseDatabase mDatabase;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private GeoQuery geoQuery;
    private DatabaseReference shopReference;
    //STRINGS
    private Float currentShopDistance;
    //MODEL
    private String dbGas, dbVul, dbBoth;
    private Shop shopModel;
    //SHOP
    private Integer radius = 9;
    private Boolean foundGas = false;
    private String shopId, shopType;
    private Double shopLat, shopLng;
    private Map<String, GeoLocation> shopArray;
    private List<String> shopKeys;
    //LAYOUT
    private RecyclerView recyclerView;
    ListShopAdapter mAdapter;
    //LOCATION
    protected FusedLocationProviderClient mFusedLocationClient;
    protected Location mLastLocation;
    protected LocationCallback locationCallback;
    protected LocationRequest mLocationRequest;
    //INTERVAL
    private long UPDATE_INTERVAL = 10 * 1000; /* 10 seconds */
    private long FASTEST_INTERVAL = 5 * 1000; /* 2 seconds */

    int counter = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_list_shop);

        initData();
        //GET SELF LOCATION
        setLocationRequest();
        setLocation();
        setOnLocationUpdate();

    }

    private void initData() {
        shopArray = new HashMap<String, GeoLocation>();
//        shopListArray = new ArrayList<>();
        shopKeys = new ArrayList<>();
        Intent i = getIntent();
        //extra
        shopType = i.getExtras().getString("shopType");
        dbGas = this.getString(R.string.db_gas);
        dbVul = this.getString(R.string.db_vul);
        dbBoth = this.getString(R.string.db_both);
        //layout
        recyclerView = findViewById(R.id.recycler_view_list_shop);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //Adapter
    }

    private void getShops() {
        DatabaseReference shopLocationRef = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("Locations")
                .child(shopType);

        GeoFire geoFire = new GeoFire(shopLocationRef);
        geoQuery = geoFire.queryAtLocation(
                new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()),
                radius
        );
        geoQuery.removeAllListeners();

        counter = 0;
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (counter >= 9) {
                    return;
                }
                shopId = key;
                Log.d("key", key + " " + location);
//                shopListArray
//                shopListArray[counter] = new ShopList(key, location);
                shopKeys.add(key);
//                shopArray.put(key, location);
                counter ++;

                if (!foundGas) foundGas = true;
            }


            @Override
            public void onKeyExited(String key) {
                Toast.makeText(
                        ViewListShopActivity.this,
                        "Shop has been removed",
                        Toast.LENGTH_SHORT
                ).show();
                startActivity(new Intent(ViewListShopActivity.this, MainPage.class));
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                Toast.makeText(
                        ViewListShopActivity.this,
                        "Shop has been moved",
                        Toast.LENGTH_SHORT
                ).show();
                startActivity(new Intent(ViewListShopActivity.this, MainPage.class));
            }

            @Override
            public void onGeoQueryReady() {
                if (radius == 10) {
                    Toast.makeText(ViewListShopActivity.this, "No Shop/s Found\n within 10km", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!foundGas) {
                    radius++;
                    getShops();
                    return;
                }
//                Log.d("key_array", shopListArray.size() + "");
                mAdapter = new ListShopAdapter(ViewListShopActivity.this, shopKeys, mLastLocation, shopType);
                recyclerView.setAdapter(mAdapter);
                Log.d("location",  shopArray + "");
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                Toast.makeText(
                        ViewListShopActivity.this,
                        "@string/db_error",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    protected void displayShopDetails(String shopId) {
        shopReference = mDatabase.getReference()
                .child("Shops").child(shopType).child(shopId);

        shopReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                shopModel = dataSnapshot.getValue(Shop.class);
                if (shopModel == null) {
                    return;
                }
//                shopName.setText(shopModel.getName());
//                shopAddress.setText(shopModel.getDescription());
//                shopLat = Double.valueOf(shopModel.getLatitude());
//                shopLng = Double.valueOf(shopModel.getLongitude());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ViewListShopActivity.this, "@string/db_error", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void setLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void setLocation() {
        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        // Check whether location settings are satisfied
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void setOnLocationUpdate() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location == null) {
                            return;
                        }
                        mLastLocation = location;
                        getShops();
                        Toast.makeText(
                                ViewListShopActivity.this,
                                "Lat: " + mLastLocation.getLatitude() +
                                        "\nLon: " + mLastLocation.getLongitude(),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
        // Used for repeating request
//        locationCallback = new LocationCallback() {
//            @Override
//            public void onLocationResult(LocationResult locationResult) {
//                super.onLocationResult(locationResult);
//                for (Location location : locationResult.getLocations()) {
//                    mLastLocation = location;
//                    Toast.makeText(
//                            ViewListShopActivity.this,
//                            "Lat: " + mLastLocation.getLatitude() +
//                                    "\nLon: " + mLastLocation.getLongitude(),
//                            Toast.LENGTH_SHORT
//                    ).show();
//                }
//            }
//        };
//        startLocationUpdates();

    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
//        mFusedLocationClient.requestLocationUpdates(
//                mLocationRequest, locationCallback, null
//        );
    }

    private void stopLocationUpdates() {
//        mFusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onStop() {
        super.onStop();
        geoQuery.removeAllListeners();
        stopLocationUpdates();
    }


    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }
}
