package com.example.weatherapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private String url = "";
    private final String API_KEY = "7498c0faeb318abb68429ab980e10fcc";
    private TextView temperatureTextView;
    private TextView weatherTextView;
    private TextView windTextView;
    private TextView sunriseTimeTextView;
    private TextView sunsetTimeTextView;
    private TextView dateTextView;
    private Button button;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private final int LOCATION_REQ_CODE = 1;
    private final int REQUEST_CHECK_SETTINGS =2;
    private ProgressBar progressBar;
    private VideoView videoView;
    private RelativeLayout relativeLayout;
    private boolean isDay=true;
    private SharedPreferences sharedPreferences;
    private String mainW;
    private boolean isCallAgain = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setWindow();

        sharedPreferences = getSharedPreferences("data_container",MODE_PRIVATE);
        button = findViewById(R.id.get_weather);
        button.setVisibility(View.GONE);

        temperatureTextView = findViewById(R.id.temperature_text);
        weatherTextView = findViewById(R.id.weather_text);
        windTextView = findViewById(R.id.wind_text);
        videoView = findViewById(R.id.video_view);
        sunriseTimeTextView = findViewById(R.id.sunrise_time);
        sunsetTimeTextView = findViewById(R.id.sunset_time);
        dateTextView = findViewById(R.id.date_text);
        progressBar = findViewById(R.id.progress_bar);
        relativeLayout = findViewById(R.id.main);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        temperatureTextView.setText(sharedPreferences.getString("temp","Temperature: get here"));
        weatherTextView.setText(sharedPreferences.getString("weather","Weather: get here"));
        windTextView.setText(sharedPreferences.getString("wind","Wind: get here"));
        sunriseTimeTextView.setText(sharedPreferences.getString("sunrise","~"));
        sunsetTimeTextView.setText(sharedPreferences.getString("sunset","~"));
        dateTextView.setText(sharedPreferences.getString("date","Date :- DD/MM/YYYY"));
        isDay = sharedPreferences.getBoolean("isDay",true);

        setBackgroundImage(sharedPreferences.getString("mainW","null"));

        animation();

        checkForPermission();
        button.setOnClickListener(view ->{
            checkForPermission();
            button.setVisibility(View.GONE);
        });

        new Thread(()->{
            while(true){
                try{
                    Thread.sleep(5000);
                    if(button.getVisibility() != View.VISIBLE){
                        LocationRequest locationRequest = LocationRequest.create();
                        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                        locationRequest.setInterval(10000);
                        locationRequest.setFastestInterval(5000);

                        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);

                        SettingsClient client = LocationServices.getSettingsClient(this);
                        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

                        task.addOnFailureListener(this,e -> {
                            isCallAgain=true;
                            button.setVisibility(View.VISIBLE);
                        });
                    }
                }catch (Exception e){}

            }
        }).start();
    }

    private void setWindow(){
        getWindow().setStatusBarColor(getResources().getColor(R.color.blue));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION  | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    void checkForPermission() {
        if(videoView.canPause())videoView.pause();
        videoView.setVisibility(View.GONE);
        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) && (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            checkLocationOn();
        } else {
            button.setVisibility(View.VISIBLE);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQ_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == LOCATION_REQ_CODE){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationOn();
            } else {
                button.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
        setWindow();
    }

    void checkLocationOn(){
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this,locationSettingsResponse -> obtainLocation());

        task.addOnFailureListener(this,e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {}
            } else {
                showLocationSettingDialog();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_CHECK_SETTINGS){
            if (resultCode == RESULT_OK){
                button.setVisibility(View.GONE);
                obtainLocation();
            }else{
                showLocationSettingDialog();
            }
        }
    }

    void showLocationSettingDialog(){
//        new AlertDialog.Builder(this)
//                .setTitle("Enable Location Services")
//                .setMessage("This app requires location services. Please enable location services in your device settings.")
//                .setPositiveButton("Settings",(dialog,which)->{
//                   Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//                   startActivity(intent);
//                    button.setVisibility(View.VISIBLE);
//                })
//                .setNegativeButton("Cancel",(dialog,which)->{
//                    Toast.makeText(this, "Location services are required for this app to function properly.", Toast.LENGTH_LONG).show();
//                    button.setVisibility(View.VISIBLE);
//                })
//                .setCancelable(false)
//                .show();


        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.location_enable_dialog);
        dialog.setCancelable(false);

        dialog.findViewById(R.id.cancel_button).setOnClickListener(view -> {
            Toast.makeText(this, "Location services are required for this app to function properly.", Toast.LENGTH_LONG).show();
            button.setVisibility(View.VISIBLE);
            dialog.dismiss();
        });
        dialog.findViewById(R.id.settings_button).setOnClickListener(view ->{
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
            button.setVisibility(View.VISIBLE);
            dialog.dismiss();
        });

        dialog.setOnCancelListener((unused)->setWindow());
        dialog.show();
    }

    @SuppressLint("MissingPermission")
    void obtainLocation() {
        progressBar.setVisibility(View.VISIBLE);
        isCallAgain=false;
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (isCallAgain){
                    fusedLocationProviderClient.removeLocationUpdates(this);
                    return;
                }
                if (locationResult == null) {
                    Log.e("WeatherApp", "Location result is null");
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        url = "https://api.openweathermap.org/data/2.5/weather?lat="+location.getLatitude()+"&lon="+location.getLongitude()+"&units=metric&appid="+API_KEY;
                        getResult();
                        progressBar.setVisibility(View.GONE);
                    }
                    break;
                }
            }
        }, getMainLooper());





    }

    void getResult(){


        RequestQueue requestQueue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.GET,url,
                response -> {
                    try{
                        JSONObject jsonObject = new JSONObject(response);

                        JSONObject main = jsonObject.getJSONObject("main");
                        String temp = main.getString("temp");

                        JSONArray weatherArray = jsonObject.getJSONArray("weather");
                        JSONObject weather = weatherArray.getJSONObject(0);
                        mainW = weather.getString("main");

                        JSONObject wind = jsonObject.getJSONObject("wind");
                        String speed = wind.getString("speed");

                        JSONObject time = jsonObject.getJSONObject("sys");
                        long sunriseTime = Long.parseLong(time.getString("sunrise"));
                        long sunsetTime = Long.parseLong(time.getString("sunset"));

                        Log.d("TAG", "getResult: "+time);
                        String[] sunriseDateTime = getDateTime(sunriseTime);
                        String[] sunsetDateTime = getDateTime(sunsetTime);


                        temperatureTextView.setText("Temperature: "+temp+"°C");
                        weatherTextView.setText("Weather: "+mainW);
                        windTextView.setText("Wind: "+speed+"km/h");

                        LocalDateTime sunrisetime,sunsettime,curruntTime;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            sunrisetime = LocalDateTime.ofInstant(Instant.ofEpochSecond(sunriseTime), ZoneId.systemDefault());
                            sunsettime = LocalDateTime.ofInstant(Instant.ofEpochSecond(sunsetTime),ZoneId.systemDefault());
                            curruntTime=LocalDateTime.now();

                            if(curruntTime.isAfter(sunrisetime) && curruntTime.isBefore(sunsettime))
                                isDay=true;
                            else
                                isDay = false;
                        }


                        sunriseTimeTextView.setText(sunriseDateTime[1]);
                        sunsetTimeTextView.setText(sunsetDateTime[1]);
                        dateTextView.setText("Date :- "+sunriseDateTime[0]);

                        setBackgroundImage(mainW);
                        dataAnimation();

                        SharedPreferences.Editor editor= sharedPreferences.edit();
                        editor.putString("temp",temperatureTextView.getText().toString());
                        editor.putString("weather",weatherTextView.getText().toString());
                        editor.putString("wind",windTextView.getText().toString());
                        editor.putBoolean("isDay",isDay);
                        editor.putString("mainW",mainW);
                        editor.putString("sunrise",sunriseDateTime[1]);
                        editor.putString("sunset",sunsetDateTime[1]);
                        editor.putString("date","Date :- "+sunriseDateTime[0]);
                        editor.apply();

                    }catch (Exception e){}
                },
                error -> {
                    Toast.makeText(this,"Failed to get data",Toast.LENGTH_SHORT).show();

                });

        requestQueue.add(stringRequest);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    void setBackgroundImage(String value){
        if(isDay){
            switch (value){
                case "Rain":
                    videoView.setVisibility(View.VISIBLE);

                    Uri videoUri = Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.rain_video);
                    videoView.setVideoURI(videoUri);
                    videoView.start();
                    videoView.setOnPreparedListener(mediaPlayer -> mediaPlayer.setLooping(true));
                    break;
                case "Clouds":
                    relativeLayout.setBackground(getResources().getDrawable(R.drawable.clouds));
                    break;
                case "Clear":
                    relativeLayout.setBackground(getResources().getDrawable(R.drawable.clear));
                    break;
                case "Drizzle":
                    relativeLayout.setBackground(getResources().getDrawable(R.drawable.drizzle));
                    break;
                case "Thunderstorm":
                    relativeLayout.setBackground(getResources().getDrawable(R.drawable.thunderstorm));
                    break;
                case "Snow":
                    relativeLayout.setBackground(getResources().getDrawable(R.drawable.snow));
                    break;
                case "Mist":
                    relativeLayout.setBackground(getResources().getDrawable(R.drawable.mist));
                    break;
                case "Tornado":
                    relativeLayout.setBackground(getResources().getDrawable(R.drawable.tornado));
                    break;
                case "Dry":
                    relativeLayout.setBackground(getResources().getDrawable(R.drawable.dry));
                    break;
                case "Fog":
                    relativeLayout.setBackground(getResources().getDrawable(R.drawable.fog));
                    break;
                default:
                    relativeLayout.setBackground(getResources().getDrawable(R.drawable.day));
                    break;
            }
        }else{
            switch (value){
                case "Rain":
                    videoView.setVisibility(View.VISIBLE);

                    Uri videoUri = Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.rain_video);
                    videoView.setVideoURI(videoUri);
                    videoView.start();
                    videoView.setOnPreparedListener(mediaPlayer -> mediaPlayer.setLooping(true));
                    break;
                case "Clouds":
                    relativeLayout.setBackground(getResources().getDrawable(R.drawable.nclouds));
                    break;
                case "Clear":
                    relativeLayout.setBackground(getResources().getDrawable(R.drawable.night));
                    break;
                case "Tornado":
                    relativeLayout.setBackground(getResources().getDrawable(R.drawable.ntornado));
                    break;
                default:
                    relativeLayout.setBackground(getResources().getDrawable(R.drawable.ndefaults));
                    break;
            }
        }
    }

    String[] getDateTime(long time){
        Date dateTime = new Date(time*1000);

        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String dateTimeString = sdf.format(dateTime);
        String[] result = dateTimeString.split(" ");

        String[] t = result[1].split(":");
        if(Integer.parseInt(t[0]) > 12 ){
            t[0]=(Integer.parseInt(t[0])-12+"");
            t[1]=t[1]+" PM";
        }else{
            t[1]=t[1]+" AM";
        }
        result[1] = t[0]+":"+t[1];

        return result;
    }

    void dataAnimation(){
        Animation rotate = AnimationUtils.loadAnimation(this,R.anim.data_info_anim);
        temperatureTextView.startAnimation(rotate);
        weatherTextView.startAnimation(rotate);
        windTextView.startAnimation(rotate);

        Animation translate = AnimationUtils.loadAnimation(this,R.anim.data_symbole_anim);
        findViewById(R.id.temperature_img).startAnimation(translate);
        findViewById(R.id.weather_img).startAnimation(translate);
        findViewById(R.id.wind_img).startAnimation(translate);
    }

    void animation(){
       View leftAnim;
       View rightAnim;
       View leftRotateAnim;
       View rightRotateAnim;

        leftAnim = findViewById(R.id.left_anim);
        rightAnim = findViewById(R.id.right_anim);
        leftRotateAnim = findViewById(R.id.left_rotate_anim);
        rightRotateAnim = findViewById(R.id.right_rotate_anim);
        Animation translate = AnimationUtils.loadAnimation(this,R.anim.translate_animation);
        leftAnim.startAnimation(translate);
        rightAnim.startAnimation(translate);

        Animation rotate = AnimationUtils.loadAnimation(this,R.anim.rotate_animation);
        leftRotateAnim.startAnimation(rotate);
        rightRotateAnim.startAnimation(rotate);
    }
}