package com.dji.GSDemo.GoogleMap;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;
import dji.thirdparty.afinal.core.AsyncTask;
import dji.waypointv2.common.waypointv1.LocationCoordinate2D;

public class CreateRouteActivityAutomatic extends FragmentActivity implements View.OnClickListener, GoogleMap.OnMapClickListener, OnMapReadyCallback {

    protected static final String TAG = "GSDemoActivity";

    private GoogleMap gMap;

    private Button locate, add, clear;
    private Button config, upload, start, stop;

    private boolean isAdd = false;

    private double droneLocationLat = 181, droneLocationLng = 181;
    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();
    private Marker droneMarker = null;

    private float altitude = 100.0f;

    private double centerLat = 100.0f;

    private double centerLng = 100.0f;

    private int radius = 1;

    private int waypointCount = 0;
    private float mSpeed = 10.0f;

    private List<Waypoint> waypointList = new ArrayList<>();

    public static WaypointMission.Builder waypointMissionBuilder;
    private FlightController mFlightController;
    private WaypointMissionOperator instance;

    private boolean directionC = false;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.AUTO;

    @Override
    protected void onResume(){
        super.onResume();
        initFlightController();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        unregisterReceiver(mReceiver);
        removeListener();
        super.onDestroy();
    }

    /**
     * @Description : RETURN Button RESPONSE FUNCTION
     */
    public void onReturn(View view){
        Log.d(TAG, "onReturn");
        this.finish();
    }

    private void setResultToToast(final String string){
        CreateRouteActivityAutomatic.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CreateRouteActivityAutomatic.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initUI() {

        locate = (Button) findViewById(R.id.locate);
        add = (Button) findViewById(R.id.add);
        clear = (Button) findViewById(R.id.clear);
        config = (Button) findViewById(R.id.config);
        upload = (Button) findViewById(R.id.upload);
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);

        locate.setOnClickListener(this);
        add.setOnClickListener(this);
        clear.setOnClickListener(this);
        config.setOnClickListener(this);
        upload.setOnClickListener(this);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // When the compile and target version is higher than 22, please request the
        // following permissions at runtime to ensure the
        // SDK work well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }

        setContentView(R.layout.activity_waypoint1);

        IntentFilter filter = new IntentFilter();
        filter.addAction(DJIDemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        initUI();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        addListener();

    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };

    private void onProductConnectionChange()
    {
        initFlightController();
        loginAccount();
    }

    private void loginAccount(){

        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Log.e(TAG, "Login Success");
                    }
                    @Override
                    public void onFailure(DJIError error) {
                        setResultToToast("Login Error:"
                                + error.getDescription());
                    }
                });
    }

    private void initFlightController() {

        BaseProduct product = DJIDemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                mFlightController = ((Aircraft) product).getFlightController();
            }
        }

        if (mFlightController != null) {
            mFlightController.setStateCallback(new FlightControllerState.Callback() {

                @Override
                public void onUpdate(FlightControllerState djiFlightControllerCurrentState) {
                    droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                    droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
                    updateDroneLocation();
                }
            });
        }
    }

    //Add Listener for WaypointMissionOperator
    private void addListener() {
        if (getWaypointMissionOperator() != null){
            getWaypointMissionOperator().addListener(eventNotificationListener);
        }
    }

    private void removeListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().removeListener(eventNotificationListener);
        }
    }

    private WaypointMissionOperatorListener eventNotificationListener = new WaypointMissionOperatorListener() {
        @Override
        public void onDownloadUpdate(WaypointMissionDownloadEvent downloadEvent) {

        }

        @Override
        public void onUploadUpdate(WaypointMissionUploadEvent uploadEvent) {

        }

        @Override
        public void onExecutionUpdate(WaypointMissionExecutionEvent executionEvent) {

        }

        @Override
        public void onExecutionStart() {

        }

        @Override
        public void onExecutionFinish(@Nullable final DJIError error) {
            setResultToToast("Execution finished: " + (error == null ? "Success!" : error.getDescription()));
        }
    };

    public WaypointMissionOperator getWaypointMissionOperator() {
        if (instance == null) {
            if (DJISDKManager.getInstance().getMissionControl() != null){
                instance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
            }
        }
        return instance;
    }

    private void setUpMap() {
        gMap.setOnMapClickListener(this);// add the listener for click for amap object

    }

    private List<Waypoint> generateWaypointsForCircle(double centerLat,double centerLng, int radius, int waypointCount) {
        List<Waypoint> waypoints = new ArrayList<>();
        double angleIncrement = 2 * Math.PI / waypointCount;
        double earthR = 6378140;
        System.out.println("Antes de la lista");
        System.out.println("Latitude home " + centerLat);
        System.out.println("Longitude home" + centerLng);
        System.out.println("Altitude home" + altitude);
        for (int i = 0; i < waypointCount; i++) {
            System.out.println("Waypoint " + i);
            double angle = i * angleIncrement;
            double Z =  radius * Math.sin(angle);
            double W = radius * Math.cos(angle);

            double longF = centerLng + (W/earthR) * ((180/Math.PI)/Math.cos(centerLat * (Math.PI/180))) ;
            double latF = centerLat + (Z/earthR) * (180/Math.PI);

            Waypoint waypoint = new Waypoint(latF, longF, altitude);
            waypoints.add(waypoint);
            System.out.println("Altitude way" + altitude);
            System.out.println("End Waypoint " + i);
        }
        System.out.println("Waypoints " + waypoints);

        if (directionC){
            Collections.reverse(waypoints);
        }

        return waypoints;
    }



    @Override
    public void onMapClick(LatLng latLng) {
        if (isAdd) {


        } else {
            setResultToToast("Cannot Add Waypoint");
        }
    }







    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    // Update the drone location based on states from MCU.
    private void updateDroneLocation(){

        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        //Create MarkerOptions object
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(pos);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (droneMarker != null) {
                    droneMarker.remove();
                }

                if (checkGpsCoordination(droneLocationLat, droneLocationLng)) {
                    droneMarker = gMap.addMarker(markerOptions);
                }
            }
        });
    }

    private void markWaypoint(LatLng point){
        // Crear objeto MarkerOptions
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        Marker marker = gMap.addMarker(markerOptions);
        mMarkers.put(mMarkers.size(), marker);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.locate:{
                updateDroneLocation();
                cameraUpdate(); // Locate the drone's place
                break;
            }
            case R.id.add:{
                enableDisableAdd();
                break;
            }
            case R.id.clear:{
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        gMap.clear();
                    }

                });
                waypointList.clear();
                waypointMissionBuilder.waypointList(waypointList);
                updateDroneLocation();
                break;
            }
            case R.id.config:{
                showSettingDialog();
                break;
            }
            case R.id.upload:{
                uploadWayPointMission();
                break;
            }
            case R.id.start:{
                startWaypointMission();
                break;
            }
            case R.id.stop:{
                stopWaypointMission();
                break;
            }
            default:
                break;
        }
    }

    private void cameraUpdate(){
        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        float zoomlevel = (float) 18.0;
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, zoomlevel);
        gMap.moveCamera(cu);

    }

    private void enableDisableAdd(){
        if (isAdd == false) {
            isAdd = true;
            add.setText("Exit");
        }else{
            isAdd = false;
            add.setText("Add");
        }
    }

    private void showSettingDialog(){

        SharedPreferences sharedPreferences = getSharedPreferences("Cache", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();


        centerLat = droneLocationLat;
        centerLng = droneLocationLng;
        int directionid = sharedPreferences.getInt("DIRECTIONC",0);
        int speedid = sharedPreferences.getInt("MSPEED",0);
        int finishid = sharedPreferences.getInt("FINISHACTION",0);
        int headingid = sharedPreferences.getInt("HEADING",0);
        int radioCache= sharedPreferences.getInt("RADIUS",0);
        int waypointcountCache= sharedPreferences.getInt("WAYPOINTCOUNT",0);
        int altitudeCache = (int) sharedPreferences.getFloat("ALTITUDE",0);
        String latCache = sharedPreferences.getString("CENTERLAT","");
        String lngCache = sharedPreferences.getString("CENTERLNG","");



        ScrollView wayPointSettings = (ScrollView) getLayoutInflater().inflate(R.layout.dialog_waypoint3setting, null);

        final TextView centerLatEditText = (TextView) wayPointSettings.findViewById(R.id.centerLat);
        final TextView centerLngEditText = (TextView) wayPointSettings.findViewById(R.id.centerLng);
        final TextView radiusEditText = (TextView) wayPointSettings.findViewById(R.id.radius);
        final TextView waypointCountSpinner = (TextView) wayPointSettings.findViewById(R.id.waypointCount);
        final TextView wpAltitude_TV = (TextView) wayPointSettings.findViewById(R.id.altitude);
        RadioGroup direction = (RadioGroup) wayPointSettings.findViewById(R.id.direction);
        RadioGroup speed_RG = (RadioGroup) wayPointSettings.findViewById(R.id.speed);
        RadioGroup actionAfterFinished_RG = (RadioGroup) wayPointSettings.findViewById(R.id.actionAfterFinished);
        RadioGroup heading_RG = (RadioGroup) wayPointSettings.findViewById(R.id.heading);

        // Establecer los valores en los TextView
        if(!latCache.equals("")){
            centerLatEditText.setText(latCache);
        }else {
            centerLatEditText.setText(String.valueOf(centerLat));
        }
        if(!lngCache.equals("")){
            centerLngEditText.setText(lngCache);
        }else {
            centerLngEditText.setText(String.valueOf(centerLng));
        }



        if(radioCache>0){
            radiusEditText.setText(String.valueOf(radioCache));
        }
        if(waypointcountCache>0){
            waypointCountSpinner.setText(String.valueOf(waypointcountCache));
        }
        if(altitudeCache>0){
            wpAltitude_TV.setText(String.valueOf(altitudeCache));
        }
        if(directionid>0)
        {
            direction.check(directionid);
        }
        if(speedid>0)
        {
            speed_RG.check(speedid);
        }
        if(finishid>0)
        {
            actionAfterFinished_RG.check(finishid);
        }
        if(headingid>0)
        {
            heading_RG.check(headingid);
        }
        direction.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.direction_clock){
                    directionC = true;
                } else if (checkedId == R.id.direction_noclock){
                    directionC = false;
                }
                editor.putInt("DIRECTIONC", checkedId);
                editor.apply();
            }
        });

        speed_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.lowSpeed){
                    mSpeed = 3.0f;
                } else if (checkedId == R.id.MidSpeed){
                    mSpeed = 5.0f;
                } else if (checkedId == R.id.HighSpeed){
                    mSpeed = 10.0f;
                }
                editor.putInt("MSPEED", checkedId);
                editor.apply();
            }

        });

        actionAfterFinished_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Select finish action");
                if (checkedId == R.id.finishNone){
                    mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
                } else if (checkedId == R.id.finishGoHome){
                    mFinishedAction = WaypointMissionFinishedAction.GO_HOME;
                } else if (checkedId == R.id.finishAutoLanding){
                    mFinishedAction = WaypointMissionFinishedAction.AUTO_LAND;
                } else if (checkedId == R.id.finishToFirst){
                    mFinishedAction = WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
                }
                editor.putInt("FINISHACTION", checkedId);
                editor.apply();
            }
        });

        heading_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Select heading");

                if (checkedId == R.id.headingNext) {
                    mHeadingMode = WaypointMissionHeadingMode.AUTO;
                } else if (checkedId == R.id.headingInitDirec) {
                    mHeadingMode = WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
                } else if (checkedId == R.id.headingRC) {
                    mHeadingMode = WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER;
                } else if (checkedId == R.id.headingWP) {
                    mHeadingMode = WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
                }
                editor.putInt("HEADING", checkedId);
                editor.apply();
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("")
                .setView(wayPointSettings)
                .setPositiveButton("Finish",new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id) {

                        SharedPreferences sharedPreferences = getSharedPreferences("Cache", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();

                        String altitudeString = wpAltitude_TV.getText().toString();
                        altitude = Integer.parseInt(nulltoIntegerDefalt(altitudeString));

                        String latitudeString = centerLatEditText.getText().toString();
                        centerLat = Double.parseDouble(nulltoDoubleDefalt(latitudeString));

                        String longitudeString = centerLngEditText.getText().toString();
                        centerLng = Double.parseDouble(nulltoDoubleDefalt(longitudeString));

                        String radiusString = radiusEditText.getText().toString();
                        radius = Integer.parseInt(nulltoIntegerDefalt(radiusString));

                        String waypointCountString = waypointCountSpinner.getText().toString();
                        waypointCount = Integer.parseInt(nulltoIntegerDefalt(waypointCountString));

                        editor.putFloat("ALTITUDE", altitude);
                        editor.putString("CENTERLAT", ""+centerLat );
                        editor.putString("CENTERLNG", ""+centerLng );
                        editor.putInt("RADIUS", radius );
                        editor.putInt("WAYPOINTCOUNT", waypointCount);


                        editor.apply();


                        Log.e(TAG,"altitude "+altitude);
                        Log.e(TAG,"centerLat "+centerLat);
                        Log.e(TAG,"centerLng "+centerLng);
                        Log.e(TAG,"radius "+radius);
                        Log.e(TAG,"waypointCount "+waypointCount);
                        Log.e(TAG,"speed "+mSpeed);
                        Log.e(TAG, "mFinishedAction "+mFinishedAction);
                        Log.e(TAG, "mHeadingMode "+mHeadingMode);
                        configWayPointMission();


                    }

                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }

                })
                .create()
                .show();
    }

    String nulltoIntegerDefalt(String value){
        if(!isIntValue(value)) value="0";
        return value;
    }

    String nulltoDoubleDefalt(String value){
        if(!isDoubleValue(value)) value="0";
        return value;
    }

    boolean isIntValue(String val)
    {
        try {
            val=val.replace(" ","");
            Integer.parseInt(val);
        } catch (Exception e) {return false;}
        return true;
    }

    boolean isDoubleValue(String val)
    {
        try {
            val=val.replace(" ","");
            Double.parseDouble(val);
        } catch (Exception e) {return false;}
        return true;
    }

    private void configWayPointMission(){

        // Limpiar la lista de waypoints anterior
        waypointList.clear();


        // Generar waypoints para el círculo
        List<Waypoint> waypoints = generateWaypointsForCircle(centerLat, centerLng, radius, waypointCount);



        // Iterar sobre los waypoints generados
        for (Waypoint waypoint : waypoints) {
            // Marcar el waypoint en el mapa
            markWaypoint(new LatLng(waypoint.coordinate.getLatitude(), waypoint.coordinate.getLongitude()));
            if (waypointMissionBuilder != null) {
                waypointList.add(waypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }else
            {
                waypointMissionBuilder = new WaypointMission.Builder();
                waypointList.add(waypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }

            // Agregar el waypoint a la lista
            //waypointList.add(waypoint);
        }
        System.out.println("Cantidad de la lista. "+waypointList.size());

        // Actualizar la cantidad de waypoints en el constructor de la misión
        waypointMissionBuilder.waypointCount(waypointList.size());
        System.out.println("Cantidad en el Builder. "+waypointMissionBuilder.getWaypointCount());

        if (waypointMissionBuilder == null){

            waypointMissionBuilder = new WaypointMission.Builder().finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .waypointCount(waypointCount)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }else
        {
            waypointMissionBuilder.finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .waypointCount(waypointCount)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }

        System.out.println("Esta es la lista: "+ waypointMissionBuilder.getWaypointList());

        if (waypointMissionBuilder.getWaypointList().size() > 0){

            for (int i=0; i< waypointMissionBuilder.getWaypointList().size(); i++){
                Waypoint waypoint = waypointMissionBuilder.getWaypointList().get(i);
                waypoint.altitude = altitude;
                //waypoint.coordinate = new dji.common.model.LocationCoordinate2D(centerLat, centerLng); // Establecer las coordenadas del punto de ruta
            }

            setResultToToast("Set Waypoint attitude successfully");
        }

        DJIError error = getWaypointMissionOperator().loadMission(waypointMissionBuilder.build());
        if (error == null) {
            setResultToToast("loadWaypoint succeeded");
        } else {
            System.out.println("Aqui está el error:  "+error);
            setResultToToast("loadWaypoint failed " + error.getDescription());
        }
    }

    private void uploadWayPointMission(){

        getWaypointMissionOperator().uploadMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {

                    setResultToToast("Mission upload successfully!");
                    if (error == null) {
                        // Construye el objeto JSON con los datos
                        JSONObject json = new JSONObject();
                        SharedPreferences sharedPreferences = getSharedPreferences("Cache", Context.MODE_PRIVATE);
                        String password = sharedPreferences.getString("PASSWORD","");



                        try {
                            json.put("location", waypointList.get(0));
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
                            String formattedDate = dateFormat.format(new Date());
                            json.put("date", formattedDate);
                            json.put("password", password);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        int i=0;
                        try{
                            i = Integer.valueOf(new CreateRouteActivityAutomatic.PostRequestAsyncTask().execute(json).get().toString());
                            if(i==201){
                                setResultToToast("Mission Uploaded to the DataBase successfully.");
                            }

                        }catch (Exception e){
                            System.out.println("Aquí el error: "+e);
                            setResultToToast("Mission Uploaded to the aircraft successfully but It can not be store in the DataBase");
                        }}




                } else {
                    setResultToToast("Mission upload failed, error: " + error.getDescription() + " retrying...");
                    getWaypointMissionOperator().retryUploadMission(null);
                }
            }
        });

    }

    private class PostRequestAsyncTask extends AsyncTask<JSONObject, Void, Integer> {
        @Override
        protected Integer doInBackground(JSONObject... jsonObjects) {
            JSONObject json = jsonObjects[0];
            try {
                System.out.println("Entroooooooo");
                // Recuperar el valor almacenado en caché desde SharedPreferences
                SharedPreferences sharedPreferences = getSharedPreferences("Cache", Context.MODE_PRIVATE);
                int cachedValueid = sharedPreferences.getInt("IDUSER",0);

                URL url = new URL("http://3.92.66.111:80/api/usuario/"+cachedValueid+"/vuelo");

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);


                System.out.println("El Json es: "+ json.toString());

                // Write the JSON to the request body
                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(json.toString().getBytes());
                outputStream.flush();
                outputStream.close();


                return connection.getResponseCode();
            } catch (IOException e) {
                System.out.println("El hijueputa error es este: "+e);
                e.printStackTrace();
                return -1;
            }
        }
        @Override
        protected void onPostExecute(Integer responseCode) {
            if (responseCode == HttpURLConnection.HTTP_CREATED) {
                showToast("LOG IN exitoso"); // Show a success message to the user
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                showToast("Correo no existe en nuestra base de Datos");
            }else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                showToast("Contraseña no correcta");
            } else {
                showToast("Error en el registro. Código de respuesta: " + responseCode);
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

    private void startWaypointMission(){

        getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Start: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });
    }

    private void stopWaypointMission(){

        getWaypointMissionOperator().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (gMap == null) {
            gMap = googleMap;
            setUpMap();
        }

        LatLng bogota = new LatLng(4.601183, -74.06584);
        gMap.addMarker(new MarkerOptions().position(bogota).title("Marker in Bogota"));
        gMap.moveCamera(CameraUpdateFactory.newLatLng(bogota));
    }

}
