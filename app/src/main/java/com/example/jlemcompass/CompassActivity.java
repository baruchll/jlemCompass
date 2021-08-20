package com.example.jlemcompass;

import static android.view.View.INVISIBLE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Locale;

public class CompassActivity extends AppCompatActivity {
    private static final String TAG = CompassActivity.class.getSimpleName();
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private final int REQUEST_LOCATION_PERMISSION = 99;
    private Compass compass;
    private ImageView jlemIndicator;
    private ImageView imageDial;
    private TextView tvAngle;
    private TextView tvYourLocation;
    private float currentAzimuth;

    SharedPreferences prefs;
    GPSTracker gps;
    private final int RC_Permission = 1221;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

        setUserChanges(getIntent());

        checkLocationPermission();
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.ic_arrow_back));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        /////////////////////////////////////////////////
        prefs = getSharedPreferences("", MODE_PRIVATE);
        gps = new GPSTracker(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //////////////////////////////////////////
        jlemIndicator = findViewById(R.id.jlem_indicator);
        imageDial = findViewById(R.id.dial);
        tvAngle = findViewById(R.id.angle);
        tvYourLocation = findViewById(R.id.your_location);

        //////////////////////////////////////////
        jlemIndicator.setVisibility(INVISIBLE);
        jlemIndicator.setVisibility(View.GONE);

        setupCompass();
    }




    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle(R.string.title_location_permission)
                        .setMessage(R.string.text_location_permission)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(CompassActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }





    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "start compass");
        if (compass != null) {
            compass.start(this);
        }

    }


    @Override
    protected void onPause() {
        super.onPause();
        if (compass != null) {
            compass.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (compass != null) {
            compass.start(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "stop compass");
        if (compass != null) {
            compass.stop();
        }
        if (gps != null) {
            gps.stopUsingGPS();
            gps = null;
        }
    }



    private void setUserChanges(Intent intent) {
        try {
            // Toolbar Title
            ((Toolbar) findViewById(R.id.toolbar)).setTitle(
                    (intent.getExtras() != null &&
                            intent.getExtras().containsKey(Constants.TOOLBAR_TITLE)) ?
                            intent.getExtras().getString(Constants.TOOLBAR_TITLE) : getString(R.string.app_name));

            // Toolbar Title Color
            ((Toolbar) findViewById(R.id.toolbar)).setTitleTextColor(
                    (intent.getExtras() != null &&
                            intent.getExtras().containsKey(Constants.TOOLBAR_TITLE_COLOR)) ?
                            Color.parseColor(intent.getExtras().getString(Constants.TOOLBAR_TITLE_COLOR)) :
                            Color.parseColor("#" + Integer.toHexString(
                                    ContextCompat.getColor(this, android.R.color.white))));

            // Toolbar Background Color
            findViewById(R.id.toolbar).setBackgroundColor(
                    (intent.getExtras() != null &&
                            intent.getExtras().containsKey(Constants.TOOLBAR_BG_COLOR)) ?
                            Color.parseColor(intent.getExtras().getString(Constants.TOOLBAR_BG_COLOR)) :
                            Color.parseColor("#" + Integer.toHexString(
                                    ContextCompat.getColor(this, R.color.app_red))));



            // Root Background Color
            findViewById(R.id.root).setBackgroundColor(
                    (intent.getExtras() != null &&
                            intent.getExtras().containsKey(Constants.COMPASS_BG_COLOR)) ?
                            Color.parseColor(intent.getExtras().getString(Constants.COMPASS_BG_COLOR)) :
                            Color.parseColor("#" + Integer.toHexString(
                                    ContextCompat.getColor(this, R.color.app_red))));

            // jlem Degrees Text Color
            ((TextView) findViewById(R.id.angle)).setTextColor(
                    (intent.getExtras() != null &&
                            intent.getExtras().containsKey(Constants.ANGLE_TEXT_COLOR)) ?
                            Color.parseColor(intent.getExtras().getString(Constants.ANGLE_TEXT_COLOR)) :
                            Color.parseColor("#" + Integer.toHexString(
                                    ContextCompat.getColor(this, android.R.color.white))));

            // Dial
            ((ImageView) findViewById(R.id.dial)).setImageResource(
                    (intent.getExtras() != null &&
                            intent.getExtras().containsKey(Constants.DRAWABLE_DIAL)) ?
                            intent.getExtras().getInt(Constants.DRAWABLE_DIAL) : R.drawable.dial);

            // Qibla Indicator
            ((ImageView) findViewById(R.id.jlem_indicator)).setImageResource(
                    (intent.getExtras() != null &&
                            intent.getExtras().containsKey(Constants.DRAWABLE_jlem)) ?
                            intent.getExtras().getInt(Constants.DRAWABLE_jlem) : R.drawable.jlem);

            // Footer Image
            findViewById(R.id.footer_image).setVisibility(
                    (intent.getExtras() != null &&
                            intent.getExtras().containsKey(Constants.FOOTER_IMAGE_VISIBLE)) ?
                            intent.getExtras().getInt(Constants.FOOTER_IMAGE_VISIBLE) : View.VISIBLE);

            // Your Location TextView
            findViewById(R.id.your_location).setVisibility(
                    (intent.getExtras() != null &&
                            intent.getExtras().containsKey(Constants.LOCATION_TEXT_VISIBLE)) ?
                            intent.getExtras().getInt(Constants.LOCATION_TEXT_VISIBLE) : View.VISIBLE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupCompass() {
        Boolean permission_granted = GetBoolean("permission_granted");
        if (permission_granted) {
            getBearing();
        } else {
            tvAngle.setText(getResources().getString(R.string.msg_permission_not_granted_yet));
            tvYourLocation.setText(getResources().getString(R.string.msg_permission_not_granted_yet));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION},
                        RC_Permission);
            } else {
                fetch_GPS();
            }
        }


        compass = new Compass(this);
        Compass.CompassListener cl = new Compass.CompassListener() {

            @Override
            public void onNewAzimuth(float azimuth) {
                // adjustArrow(azimuth);
                adjustGambarDial(azimuth);
                adjustArrowjlem(azimuth);
            }
        };
        compass.setListener(cl);

        ////////////// ADDED CODE ///////////////
        //fetch_GPS();
    }

    private void adjustGambarDial(float azimuth) {
        Animation an = new RotateAnimation(-currentAzimuth, -azimuth,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        currentAzimuth = (azimuth);
        an.setDuration(500);
        an.setRepeatCount(0);
        an.setFillAfter(true);
        imageDial.startAnimation(an);
    }

    private void adjustArrowjlem(float azimuth) {
        float jlem_degrees = GetFloat("jlem degrees");
        Animation an = new RotateAnimation(-(currentAzimuth) + jlem_degrees, -azimuth,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        currentAzimuth = (azimuth);
        an.setDuration(500);
        an.setRepeatCount(0);
        an.setFillAfter(true);
        jlemIndicator.startAnimation(an);
        if (jlem_degrees > 0) {
            jlemIndicator.setVisibility(View.VISIBLE);
        } else {
            jlemIndicator.setVisibility(INVISIBLE);
            jlemIndicator.setVisibility(View.GONE);
        }
    }

    @SuppressLint("MissingPermission")
    public void getBearing() {
        // Get the location manager

        float jlemDegs = GetFloat("jlem degrees");
        if (jlemDegs > 0.0001) {
            String strYourLocation;
            if(gps.getLocation() != null)
                strYourLocation = getResources().getString(R.string.your_location)
                        + " " + gps.getLocation().getLatitude() + ", " + gps.getLocation().getLongitude();
            else
                strYourLocation = getResources().getString(R.string.unable_to_get_your_location);
            tvYourLocation.setText(strYourLocation);
            String strJlemDirection = String.format(Locale.ENGLISH, "%.0f", jlemDegs)
                    + " " + getResources().getString(R.string.degree) + " " + getDirectionString(jlemDegs);
            tvAngle.setText(strJlemDirection);

            jlemIndicator.setVisibility(View.VISIBLE);
        } else {
            fetch_GPS();
        }
    }

    private String getDirectionString(float azimuthDegrees) {
        String where = "NW";

        if (azimuthDegrees >= 350 || azimuthDegrees <= 10)
            where = "N";
        if (azimuthDegrees < 350 && azimuthDegrees > 280)
            where = "NW";
        if (azimuthDegrees <= 280 && azimuthDegrees > 260)
            where = "W";
        if (azimuthDegrees <= 260 && azimuthDegrees > 190)
            where = "SW";
        if (azimuthDegrees <= 190 && azimuthDegrees > 170)
            where = "S";
        if (azimuthDegrees <= 170 && azimuthDegrees > 100)
            where = "SE";
        if (azimuthDegrees <= 100 && azimuthDegrees > 80)
            where = "E";
        if (azimuthDegrees <= 80 && azimuthDegrees > 10)
            where = "NE";

        return where;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_Permission) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, yay! Do the
                SaveBoolean("permission_granted", true);
                tvAngle.setText(getResources().getString(R.string.msg_permission_granted));
                tvYourLocation.setText(getResources().getString(R.string.msg_permission_granted));
                jlemIndicator.setVisibility(INVISIBLE);
                jlemIndicator.setVisibility(View.GONE);

                fetch_GPS();
            } else {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.toast_permission_required), Toast.LENGTH_LONG).show();
                finish();
            }
        }

    }







    private void SaveBoolean(String title, boolean bbb) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(title, bbb);
        edit.apply();
    }

    private Boolean GetBoolean(String title) {
        return prefs.getBoolean(title, false);
    }

    public void SaveFloat(String title, Float bbb) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putFloat(title, bbb);
        edit.apply();
    }



    public Float GetFloat(String title) {
        return prefs.getFloat(title, 0);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);


    }


    public void fetch_GPS() {
        double result;
        gps = new GPSTracker(this);
        if (gps.canGetLocation()) {
            double myLat = gps.getLatitude();
            double myLng = gps.getLongitude();
            // \n is for new line
            String strYourLocation = getResources().getString(R.string.your_location)
                    + " " + myLat + ", " + myLng;
            tvYourLocation.setText(strYourLocation);
            Log.e("TAG", "GPS is on");
            if (myLat < 0.001 && myLng < 0.001) {
                jlemIndicator.setVisibility(INVISIBLE);
                jlemIndicator.setVisibility(View.GONE);
                tvAngle.setText(getResources().getString(R.string.location_not_ready));
                tvYourLocation.setText(getResources().getString(R.string.location_not_ready));

                // Toast.makeText(getApplicationContext(), "Location not ready, Please Restart Application", Toast.LENGTH_LONG).show();
            } else {

                double kotelLng = 39.826206;
                double kotelLat = Math.toRadians(21.422487);
                double myLatRad = Math.toRadians(myLat);
                double longDiff = Math.toRadians(kotelLng - myLng);
                double y = Math.sin(longDiff) * Math.cos(kotelLat);
                double x = Math.cos(myLatRad) * Math.sin(kotelLat) - Math.sin(myLatRad) * Math.cos(kotelLat) * Math.cos(longDiff);
                result = (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
                SaveFloat("jlem degrees", (float) result);
                String strKaabaDirection = String.format(Locale.ENGLISH, "%.0f", (float) result)
                        + " " + getResources().getString(R.string.degree) + " " + getDirectionString((float) result);
                tvAngle.setText(strKaabaDirection);
                jlemIndicator.setVisibility(View.VISIBLE);


            }
        } else {
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gps.showSettingsAlert();

            jlemIndicator.setVisibility(INVISIBLE);
            jlemIndicator.setVisibility(View.GONE);
            tvAngle.setText(getResources().getString(R.string.pls_enable_location));
            tvYourLocation.setText(getResources().getString(R.string.pls_enable_location));

            // Toast.makeText(getApplicationContext(), "Please enable Location first and Restart Application", Toast.LENGTH_LONG).show();
        }
    }






}

