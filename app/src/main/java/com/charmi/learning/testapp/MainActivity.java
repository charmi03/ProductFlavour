package com.charmi.learning.testapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.charmi.learning.testapp.POJO.Example;
import com.charmi.learning.testapp.POJO.Photo;
import com.charmi.learning.testapp.POJO.Result;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static android.support.design.widget.BottomSheetBehavior.STATE_COLLAPSED;
import static android.support.design.widget.BottomSheetBehavior.STATE_EXPANDED;

public class MainActivity extends AppCompatActivity implements ItemAdapter.ItemListener {

    BottomSheetBehavior behavior;
    RecyclerView recyclerView;
    private ItemAdapter mAdapter;
    private TextView tvResult;
    private ArrayList<Result> infoList;
    public int PROXIMITY_RADIUS = 0;

    private static final String TAG = MainActivity.class.getSimpleName();

    private SupportMapFragment mapFragment;
    private GoogleMap map;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;
    private Location mCurrentLocation;
    private Boolean mRequestingLocationUpdates;
    boolean isMapVisible;
    private ProgressBar progress_bar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
        if (mapFragment != null) {
            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap map) {
                    loadMap(map);
                }
            });
        } else {
            Toast.makeText(this, "Error - Map Fragment was null!!", Toast.LENGTH_SHORT).show();
        }

        tvResult = findViewById(R.id.tvResult);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mRequestingLocationUpdates = true;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        progress_bar = findViewById(R.id.progress_bar);

        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();

        View bottomSheet = findViewById(R.id.bottom_sheet);
        behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                // React to state change
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // React to dragging event
            }
        });

        tvResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (infoList != null && infoList.size() > 0) {
                    behavior.setState(STATE_EXPANDED);
                } else {
                    behavior.setState(STATE_COLLAPSED);
                    Toast.makeText(MainActivity.this, "No result found !!", Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

    protected void loadMap(GoogleMap googleMap) {
        map = googleMap;
        if (map != null) {

        } else {
            Toast.makeText(this, "Error - Map was null!!", Toast.LENGTH_SHORT).show();
        }
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                mCurrentLocation = locationResult.getLastLocation();
                updateLocationUI();
            }
        };
    }

    private void updateLocationUI() {

        progress_bar.setVisibility(View.GONE);

        if (mCurrentLocation != null) {

            LatLng latLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());

            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17);
            if (map != null) {
                map.setMyLocationEnabled(true);
                map.animateCamera(cameraUpdate);

            }

            if (getPreferencesRadius("RADIUS") > 0)
                PROXIMITY_RADIUS = getPreferencesRadius("RADIUS");
            else
                PROXIMITY_RADIUS = 1500;

            retrofit_call(getResources().getString(R.string.place_name), PROXIMITY_RADIUS);

            stopLocationUpdates();

        }
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        mRequestingLocationUpdates = false;
                        updateLocationUI();
                        break;
                }
                break;
        }
    }

    private void startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");

                        //noinspection MissingPermission
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                            return;
                        }
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());

                        updateLocationUI();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);
                                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                mRequestingLocationUpdates = false;
                        }

                        updateLocationUI();
                    }
                });
    }

    private void stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Log.d(TAG, "stopLocationUpdates: updates never requested, no-op.");
            return;
        }

        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mRequestingLocationUpdates = false;
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mCurrentLocation != null) {
            progress_bar.setVisibility(View.VISIBLE);

            updateLocationUI();
        } else if (mRequestingLocationUpdates && checkPermissions()) {
            startLocationUpdates();
        } else if (!checkPermissions()) {
            requestPermissions();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        progress_bar.setVisibility(View.GONE);
        stopLocationUpdates();
    }


    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");

            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {

                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mRequestingLocationUpdates) {
                    Log.i(TAG, "Permission granted, updates requested, starting location updates");
                    startLocationUpdates();
                }
            } else {

                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }

    private boolean isNetworkConnected() {

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm != null) {
            return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
        }
        return false;
    }

    private void retrofit_call(String type, int radius) {

        if (!isNetworkConnected()) {
            Toast.makeText(MainActivity.this, "No internet connection found !!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mCurrentLocation != null) {
            progress_bar.setVisibility(View.VISIBLE);

            String url = "https://maps.googleapis.com/maps/";

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(url)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            RetrofitMaps service = retrofit.create(RetrofitMaps.class);

            Call<Example> call = service.getNearbyPlaces(type, mCurrentLocation.getLatitude() + "," +
                    mCurrentLocation.getLongitude(), radius);

            call.enqueue(new Callback<Example>() {
                @Override
                public void onResponse(Call<Example> call, Response<Example> response) {
                    try {
                        map.clear();
                        progress_bar.setVisibility(View.GONE);

                        infoList = new ArrayList<>();

                        // This loop will go through all the results and add marker on each location.
                        if (response.body().getResults().size() > 0) {

                            for (int i = 0; i < response.body().getResults().size(); i++) {
                                Double lat = response.body().getResults().get(i).getGeometry().getLocation().getLat();
                                Double lng = response.body().getResults().get(i).getGeometry().getLocation().getLng();
                                String placeName = response.body().getResults().get(i).getName();
                                String vicinity = response.body().getResults().get(i).getVicinity();
                                Double rating = response.body().getResults().get(i).getRating();
                                String icon = response.body().getResults().get(i).getIcon();
                                String reference = response.body().getResults().get(i).getReference();
                                List<Photo> photos = response.body().getResults().get(i).getPhotos();
                                LatLng latLng = new LatLng(lat, lng);

                                MarkerOptions markerOptions = new MarkerOptions();
                                markerOptions.position(latLng)
                                        .title(placeName)
                                        .snippet(vicinity)
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

                                Result info = new Result();
                                info.setName(placeName);
                                info.setVicinity(vicinity);
                                info.setIcon(icon);
                                info.setRating(rating);
                                info.setReference(reference);
                                info.setPhotos(photos);
                                infoList.add(info);
                                CustomInfoWindowGoogleMap customInfoWindow = new CustomInfoWindowGoogleMap(MainActivity.this);
                                map.setInfoWindowAdapter(customInfoWindow);

                                Marker m = map.addMarker(markerOptions);
                                m.setTag(info);
                                m.showInfoWindow();

                                map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                                map.animateCamera(CameraUpdateFactory.zoomTo(13));

                            }
                        } else {
                            behavior.setState(STATE_COLLAPSED);
                            Toast.makeText(MainActivity.this, "Status: " + response.body().getStatus() + "No result found !!", Toast.LENGTH_SHORT).show();
                        }

                        setBottomSheetAdapter(getApplicationContext(), infoList);

                    } catch (Exception e) {
                        Log.d("onResponse", "There is an error");
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(Call<Example> call, Throwable t) {
                    Log.d("onFailure", t.toString());
                }

            });
        } else {
            Toast.makeText(MainActivity.this, "Please wait..its taking longer to fetch location", Toast.LENGTH_SHORT).show();
            startLocationUpdates();
        }

    }

    @Override
    public void onBackPressed() {

        if (behavior != null && behavior.getState() == STATE_EXPANDED)
            behavior.setState(STATE_COLLAPSED);
        else
            super.onBackPressed();

    }

    private void setBottomSheetAdapter(Context ctx, List<Result> infoList) {
        mAdapter = new ItemAdapter(ctx, this, infoList);
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.action_radius:
                showAlert(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void SavePreferencesRadius(String key, int value) {
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    private int getPreferencesRadius(String key) {
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        int savedPref = sharedPreferences.getInt(key, 0);

        return savedPref;
    }

    private void showAlert(Context ctx) {
        LayoutInflater li = LayoutInflater.from(ctx);
        View promptsView = li.inflate(R.layout.alert_layout, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx);
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = promptsView.findViewById(R.id.et_radius);

        if (getPreferencesRadius("RADIUS") > 0)
            userInput.setText(String.valueOf(getPreferencesRadius("RADIUS")));

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("Set",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                if (!TextUtils.isEmpty(userInput.getText())) {
                                    SavePreferencesRadius("RADIUS", Integer.parseInt(userInput.getText().toString()));
                                    PROXIMITY_RADIUS = Integer.parseInt(userInput.getText().toString());
                                    retrofit_call(getResources().getString(R.string.place_name), PROXIMITY_RADIUS);
                                }

                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }

    @Override
    public void onItemClick(String item) {
        behavior.setState(STATE_COLLAPSED);
    }


    public class CustomInfoWindowGoogleMap implements GoogleMap.InfoWindowAdapter {

        private Context context;

        public CustomInfoWindowGoogleMap(Context ctx) {
            context = ctx;
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            View view = ((Activity) context).getLayoutInflater()
                    .inflate(R.layout.map_info_window_layout, null);

            TextView name_tv = view.findViewById(R.id.name);
            TextView details_tv = view.findViewById(R.id.details);
            final ImageView img = view.findViewById(R.id.pic);
            TextView rating_tv = view.findViewById(R.id.rating);

            name_tv.setText(marker.getTitle());
            details_tv.setText(marker.getSnippet());

            Result infoWindowData = (Result) marker.getTag();
//
            Glide.with(getApplicationContext())
                    .load(infoWindowData.getIcon())
                    .asBitmap()
                    .dontTransform()
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                            final float scale = getResources().getDisplayMetrics().density;
                            int pixels = (int) (25 * scale + 0.5f);
                            Bitmap bitmap = Bitmap.createScaledBitmap(resource, pixels, pixels, true);
                            img.setImageBitmap(bitmap);
                        }

                        @Override
                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            img.setImageBitmap(null);
                        }
                    });


            if (infoWindowData.getRating() != null)
                rating_tv.setText("Rating: " + infoWindowData.getRating().toString());

            return view;
        }
    }
}
