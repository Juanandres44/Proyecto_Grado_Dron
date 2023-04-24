package com.dji.GSDemo.GoogleMap;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

import static com.dji.GSDemo.GoogleMap.MainActivity.checkGpsCoordination;

import androidx.appcompat.app.AppCompatActivity;

public class CreateRouteActivity extends AppCompatActivity implements View.OnClickListener, OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private Button done, cancel;

    private double droneLat;
    private double droneLng;
    private ArrayList<LatLng> pathWay;
    private ArrayList<Marker> mMarkers;
    private ArrayList<String> markerIds;
    private boolean videoStarted;

    private MainActivity.TASK cameraTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.create_map);
        pathWay = new ArrayList<>();
        IntentFilter filter = new IntentFilter();
        filter.addAction(DJIDemoApplication.FLAG_CONNECTION_CHANGE);

        droneLat = getIntent().getExtras().getDouble("ID-DRONE_LAT");
        droneLng = getIntent().getExtras().getDouble("ID-DRONE_LNG");

        cameraTask = MainActivity.TASK.NONE;
        videoStarted = false;
        initUI();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.routeMap);
        mapFragment.getMapAsync(this);

    }

    private void initUI() {
        final RadioGroup tasks_RG1 =  findViewById(R.id.tasks1);
        final RadioGroup tasks_RG2 = findViewById(R.id.tasks2);

        tasks_RG1.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.tasksPicture) {
                    cameraTask = MainActivity.TASK.PICTURE;
                    tasks_RG2.clearCheck();
                } else if (checkedId == R.id.tasksVideo) {
                    cameraTask = MainActivity.TASK.VIDEO;
                    tasks_RG2.clearCheck();
                }

            }
        });

        tasks_RG2.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.tasksPanoramic) {
                    cameraTask = MainActivity.TASK.PANORAMIC;
                    tasks_RG1.clearCheck();
                } else if (checkedId == R.id.tasksInterval) {
                    cameraTask = MainActivity.TASK.INTERVAL;
                    tasks_RG1.clearCheck();
                }

            }
        });


        cancel = findViewById(R.id.btn_cancel);
        done = findViewById(R.id.btn_done);

        cancel.setOnClickListener(this);
        done.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_cancel: {

                Intent returnIntent = new Intent();
                setResult(Activity.RESULT_CANCELED, returnIntent);
                finish();
                break;

            }
            case R.id.btn_done: {

                for (Marker mMarker: mMarkers){
                    String task = "0";
                    switch (mMarker.getTag().toString()) {
                        case "PICTURE":
                            task = "1";
                            break;

                        case "VIDEO":
                            task = "2";
                            break;

                        case "INTERVAL":
                            task = "4";
                            break;

                        case "PANORAMIC":
                            task = "3";
                            break;

                        case "NONE":

                            break;
                    }
                    String indicador = mMarker.getPosition().latitude + "&" +  mMarker.getPosition().longitude + "&" +task;
                    markerIds.add(indicador);
                }
                Intent returnIntent = new Intent();
                returnIntent.putExtra("MARKED-WAYPOINTS", markerIds);
                //returnIntent.putExtra("CHOSEN-TASK", cameraTask);
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
                break;
            }
        }
    }

    private void showToast(final String toastMsg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();

            }
        });
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        String tag = (marker.getTag().toString()) ;
        switch (cameraTask) {
            case PICTURE:
                checkVideoSelectedPreviously(tag);
                processSelection(cameraTask.toString(), tag, marker, BitmapDescriptorFactory.HUE_BLUE);
                break;

            case VIDEO:
                checkVideoSelectedPreviously(tag);
                processSelection(cameraTask.toString(), tag, marker, BitmapDescriptorFactory.HUE_RED);
                videoStarted = !videoStarted;
                break;

            case INTERVAL:
                checkVideoSelectedPreviously(tag);
                processSelection(cameraTask.toString(), tag, marker, BitmapDescriptorFactory.HUE_ORANGE);
                break;

            case PANORAMIC:
                checkVideoSelectedPreviously(tag);
                processSelection(cameraTask.toString(), tag, marker, BitmapDescriptorFactory.HUE_YELLOW);
                break;

            case NONE:
                showToast("Please select a Task");
                break;
        }
        return true;
    }

    private void checkVideoSelectedPreviously(String tag){
        if(tag.equals(MainActivity.TASK.VIDEO.toString())) {
            videoStarted = !videoStarted;
        }
    }

    private void processSelection(String task, String tag, Marker marker, Float color){
        if(task.equals(tag)){
            marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));
            marker.setTag(MainActivity.TASK.NONE.toString());
        }
        else {
            marker.setIcon(BitmapDescriptorFactory.defaultMarker(color));
            marker.setTag(task);
        }
    }

    private void updateDroneLocation(){

        LatLng pos = new LatLng(droneLat, droneLng);
        //Create MarkerOptions object
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(pos);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (checkGpsCoordination(droneLat, droneLng)) {
                    mMap.addMarker(markerOptions);
                }
            }
        });

        float zoomlevel = (float) 18.0;
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, zoomlevel);
        mMap.moveCamera(cu);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (mMap == null) {
            mMap = googleMap;
            //setUpMap();
        }
        mMap.setOnMarkerClickListener(this);
        LatLng shenzhen = new LatLng(22.5362, 113.9454);
        mMap.addMarker(new MarkerOptions().position(shenzhen).title("Marker in Shenzhen"));

        updateDroneLocation();
        mMarkers = new ArrayList<Marker>();
        markerIds = new ArrayList<String>();

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                pathWay.add(point);
                markWaypoint(point);
            }
        });
    }



    private void markWaypoint(LatLng point){
        //Create MarkerOptions object
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        float color = BitmapDescriptorFactory.HUE_VIOLET;
        switch (cameraTask) {
            case PICTURE:
                checkVideoSelectedPreviously(cameraTask.toString());
                color = BitmapDescriptorFactory.HUE_BLUE;
                break;

            case VIDEO:
                checkVideoSelectedPreviously(cameraTask.toString());
                color =BitmapDescriptorFactory.HUE_RED;
                videoStarted = !videoStarted;
                break;

            case INTERVAL:
                checkVideoSelectedPreviously(cameraTask.toString());
                color = BitmapDescriptorFactory.HUE_ORANGE;
                break;

            case PANORAMIC:
                checkVideoSelectedPreviously(cameraTask.toString());
                color =BitmapDescriptorFactory.HUE_YELLOW;
                break;

            case NONE:
                break;
        }
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(color));
        Marker marker = mMap.addMarker(markerOptions);
        marker.setTag(cameraTask);
        mMarkers.add(marker);


        if(pathWay.size() > 1){
            LatLng pointer = pathWay.get(pathWay.size()-2);
            mMap.addPolyline(new PolylineOptions()
                    .add(pointer, point)
                    .width(5)
                    .color(Color.CYAN));
        }
    }

}

