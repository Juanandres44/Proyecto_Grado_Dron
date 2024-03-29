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
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.flightcontroller.FlightControllerState;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.model.LocationCoordinate2D;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.common.error.DJIError;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;
import dji.thirdparty.afinal.core.AsyncTask;

public class CreateRouteActivity extends FragmentActivity implements View.OnClickListener, GoogleMap.OnMapClickListener, OnMapReadyCallback {

    protected static final String TAG = "GSDemoActivity";

    private GoogleMap gMap;

    private Button locate, add, clear;
    private Button config, upload, start, stop,load;

    private boolean isAdd = false;

    private double droneLocationLat = 181, droneLocationLng = 181;
    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();
    private Marker droneMarker = null;

    private float altitude = 100.0f;
    private float mSpeed = 10.0f;

    private List<Waypoint> waypointList = new ArrayList<>();

    public static WaypointMission.Builder waypointMissionBuilder;
    private FlightController mFlightController;
    private WaypointMissionOperator instance;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.AUTO;

    private boolean started = false;

    private String missionCodeApp = UUID.randomUUID().toString();

    private List<Waypoint> waypointsDrone = new ArrayList<>();

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
        CreateRouteActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CreateRouteActivity.this, string, Toast.LENGTH_SHORT).show();
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
        load = (Button) findViewById(R.id.load);

        locate.setOnClickListener(this);
        add.setOnClickListener(this);
        clear.setOnClickListener(this);
        config.setOnClickListener(this);
        upload.setOnClickListener(this);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);
        load.setOnClickListener(this);

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

        setContentView(R.layout.activity_waypoint2);

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
                    if(started){
                        Waypoint waypoint1=new Waypoint(droneLocationLat,droneLocationLng,altitude);

                        if(waypointsDrone.size()>0&&      (waypointsDrone.get(waypointsDrone.size()-1).coordinate.getLatitude()!=waypoint1.coordinate.getLatitude() || waypointsDrone.get(waypointsDrone.size()-1).coordinate.getLongitude()!=waypoint1.coordinate.getLongitude())){

                            waypointsDrone.add(waypoint1);

                        }else{
                            if(waypointsDrone.size()==0)
                            {
                                waypointsDrone.add(waypoint1);
                            }
                        }
                    }else{
                        waypointsDrone.clear();
                    }
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
            hacerUpdate();
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

    @Override
    public void onMapClick(LatLng point) {
        if (isAdd == true){
            markWaypoint(point);
            Waypoint mWaypoint = new Waypoint(point.latitude, point.longitude, altitude);
            //Add Waypoints to Waypoint arraylist;
            if (waypointMissionBuilder != null) {
                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }else
            {
                waypointMissionBuilder = new WaypointMission.Builder();
                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }
        }else{
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
        //Create MarkerOptions object
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        Marker marker = gMap.addMarker(markerOptions);
        mMarkers.put(mMarkers.size(), marker);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.load:{
                System.out.println("entroooooooooooooooooooooooooooooo");
                hacerGet();
                break;
            }
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

        int speedid = 0;
        int finishid = 0;
        int headingid = 0;
        int altitudeCache = 0;




        if(missionCodeApp!=null&&missionCodeApp.toCharArray().length>0){
            System.out.println("Entroo");
            speedid = sharedPreferences.getInt(missionCodeApp+"MSPEED",0);
            finishid = sharedPreferences.getInt(missionCodeApp+"FINISHACTION",0);
            headingid = sharedPreferences.getInt(missionCodeApp+"HEADING",0);
            altitudeCache = (int) sharedPreferences.getFloat(missionCodeApp+"ALTITUDE",0);
        }else{
            speedid = sharedPreferences.getInt("MSPEED",0);
            finishid = sharedPreferences.getInt("FINISHACTION",0);
            headingid = sharedPreferences.getInt("HEADING",0);
            altitudeCache = (int) sharedPreferences.getFloat("ALTITUDE",0);
        }


        LinearLayout wayPointSettings = (LinearLayout)getLayoutInflater().inflate(R.layout.dialog_waypoint2setting, null);

        final TextView wpAltitude_TV = (TextView) wayPointSettings.findViewById(R.id.altitude);
        RadioGroup speed_RG = (RadioGroup) wayPointSettings.findViewById(R.id.speed);
        RadioGroup actionAfterFinished_RG = (RadioGroup) wayPointSettings.findViewById(R.id.actionAfterFinished);
        RadioGroup heading_RG = (RadioGroup) wayPointSettings.findViewById(R.id.heading);

        // Establecer los valores en los TextView
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
        if(altitudeCache>0){
            wpAltitude_TV.setText(String.valueOf(altitudeCache));
        }

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
                editor.putInt(missionCodeApp+"MSPEED",checkedId);
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
                editor.putInt(missionCodeApp+"FINISHACTION",checkedId);
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
                editor.putInt(missionCodeApp+"HEADING",checkedId);
                editor.apply();
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("")
                .setView(wayPointSettings)
                .setPositiveButton("Finish",new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id) {

                        String altitudeString = wpAltitude_TV.getText().toString();
                        altitude = Integer.parseInt(nulltoIntegerDefalt(altitudeString));
                        Log.e(TAG,"altitude "+altitude);
                        Log.e(TAG,"speed "+mSpeed);
                        Log.e(TAG, "mFinishedAction "+mFinishedAction);
                        Log.e(TAG, "mHeadingMode "+mHeadingMode);
                        editor.putFloat("ALTITUDE", altitude);
                        editor.putFloat(missionCodeApp+"ALTITUDE",altitude);
                        editor.apply();
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

    boolean isIntValue(String val)
    {
        try {
            val=val.replace(" ","");
            Integer.parseInt(val);
        } catch (Exception e) {return false;}
        return true;
    }

    private void configWayPointMission(){

        if (waypointMissionBuilder == null){

            waypointMissionBuilder = new WaypointMission.Builder().finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }else
        {
            waypointMissionBuilder.finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }

        if (waypointMissionBuilder.getWaypointList().size() > 0){

            for (int i=0; i< waypointMissionBuilder.getWaypointList().size(); i++){
                waypointMissionBuilder.getWaypointList().get(i).altitude = altitude;
            }

            setResultToToast("Set Waypoint attitude successfully");
        }

        DJIError error = getWaypointMissionOperator().loadMission(waypointMissionBuilder.build());
        if (error == null) {
            setResultToToast("loadWaypoint succeeded");
        } else {
            setResultToToast("loadWaypoint failed " + error.getDescription());
        }
    }

    private void uploadWayPointMission(){

        getWaypointMissionOperator().uploadMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {

                setResultToToast("Mission upload successfully!");
                if (error == null) {
                    // Construye el objeto JSON con los datos
                    JSONObject json = new JSONObject();
                    SharedPreferences sharedPreferences = getSharedPreferences("Cache", Context.MODE_PRIVATE);
                    String password = sharedPreferences.getString("PASSWORD","");
                    try {
                        json.put("locationinitLat", droneLocationLat);
                        json.put("locationinitLng", droneLocationLng);
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
                        String formattedDate = dateFormat.format(new Date());
                        json.put("date", formattedDate);
                        json.put("altitude", (double) altitude);
                        json.put("type", "Manual");
                        json.put("heading", mHeadingMode.toString());
                        json.put("finishing", mFinishedAction.toString());
                        json.put("numberWaypoints", waypointList.size());
                        json.put("speed", (double)mSpeed);

                        System.out.println(json);

                        List<LocationCoordinate2D> lista = new ArrayList<>();
                        for(int i=0; i<waypointList.size();i++){
                            lista.add(waypointList.get(i).coordinate);
                        }
                        json.put("waypointsList", lista);
                        json.put("missionCode",missionCodeApp);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    int i=0;
                    try{
                        i = Integer.valueOf(new CreateRouteActivity.PostRequestAsyncTask().execute(json).get().toString());
                        if(i==201){
                            setResultToToast("Mission Uploaded to the DataBase successfully.");
                        }

                    }catch (Exception e){
                        System.out.println("Aquí el error: "+e);
                        setResultToToast("Mission Uploaded to the aircraft successfully but It can not be store in the DataBase");
                    }
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

                URL url = new URL("http://3.208.19.176:80/api/usuario/"+cachedValueid+"/vuelo");

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

                if(connection.getResponseCode()==201) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;
                    StringBuilder response = new StringBuilder();

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();


                    JSONObject json1 = new JSONObject(response.toString());

                    // Acceder a los valores del objeto JSON
                    Integer id = json1.getInt("id");


                    sharedPreferences = getSharedPreferences("Cache", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("RESPONSE", response.toString());
                    editor.putInt("IDVUELO",id);
                    editor.apply();


                    System.out.println("Así Quedó el cache del vuelo id: "+id);
                }


                return connection.getResponseCode();
            } catch (Exception e) {
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

    private class PutRequestAsyncTask extends AsyncTask<JSONObject, Void, Integer> {
        @Override
        protected Integer doInBackground(JSONObject... jsonObjects) {
            JSONObject json = jsonObjects[0];
            try {
                System.out.println("Entroooooooo");
                // Recuperar el valor almacenado en caché desde SharedPreferences
                SharedPreferences sharedPreferences = getSharedPreferences("Cache", Context.MODE_PRIVATE);
                int cachedValueid = sharedPreferences.getInt("IDUSER",0);
                int cachedValueVuelo = sharedPreferences.getInt("IDVUELO",0);
                System.out.println("CACHE USUARIO: "+cachedValueid+"CHACHE VUELO: "+cachedValueVuelo);

                URL url = new URL("http://3.208.19.176:80/api/usuario/"+cachedValueid+"/vuelo/"+cachedValueVuelo+"/finishing");

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("PUT");
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
    public void hacerUpdate(){
        JSONObject json = new JSONObject();
        started = false;

        try {
            json.put("locationFinishLat", droneLocationLat);
            json.put("locationFinishLng", droneLocationLng);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            String formattedDate = dateFormat.format(new Date());
            json.put("finishdate", formattedDate);
            //json.put("coordinates", waypointsDrone);

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        int i=0;
        try{
            i = Integer.valueOf(new CreateRouteActivity.PutRequestAsyncTask().execute(json).get().toString());
            if(i==200){
                setResultToToast("Mission Updated to the DataBase successfully.");
            }

        }catch (Exception e){
            System.out.println("Aquí el error: "+e);
            setResultToToast("Mission Uploaded to the aircraft successfully but It can not be store in the DataBase");
        }

    }








    private void startWaypointMission(){

        getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Start: " + (error == null ? "Successfully" : error.getDescription()));
                started=true;
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

    public void hacerGet() {
        try {
            List<Mission> missions = new CreateRouteActivity.GetRequestAsyncTask().execute(new JSONObject()).get();
            if (!missions.isEmpty()) {
                // Aquí mostramos el diálogo solo si hay misiones disponibles
                showMissionListDialog(missions);
                setResultToToast("Datos traídos exitosamente.");
            } else {
                setResultToToast("No se encontraron misiones.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            setResultToToast("No se trajeron los datos.");
        }
    }


    private class GetRequestAsyncTask extends AsyncTask<JSONObject, Void, List<Mission>> {
        @Override
        protected List<Mission> doInBackground(JSONObject... jsonObjects) {
            List<Mission> missions = new ArrayList<>();
            try {
                System.out.println("Entroooooooo");
                // Recuperar el valor almacenado en caché desde SharedPreferences
                SharedPreferences sharedPreferences = getSharedPreferences("Cache", Context.MODE_PRIVATE);
                int cachedValueid = sharedPreferences.getInt("IDUSER",0);

                URL url = new URL("http://3.208.19.176:80/api/usuario/"+cachedValueid+"/vuelo");

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setConnectTimeout(5000); // Tiempo máximo de conexión en milisegundos
                connection.setReadTimeout(5000);

                if(connection.getResponseCode()==200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;
                    StringBuilder response = new StringBuilder();

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();


                    JSONArray json1 = new JSONArray(response.toString());

                    for (int i = 0; i < json1.length(); i++) {
                        String idVuelo = json1.getJSONObject(i).getString("id");
                        String tipo = json1.getJSONObject(i).getString("type");
                        String missionCode = json1.getJSONObject(i).getString("missionCode");

                        Mission mission = new Mission(idVuelo, tipo, missionCode);
                        missions.add(mission);
                    }
                }







                return missions;
            } catch (Exception e) {
                System.out.println("El hijueputa error es este: "+e);
                e.printStackTrace();
                return null;
            }
        }

    }



    private void showMissionListDialog(List<Mission> missions) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.mission_list, null);
        dialogBuilder.setView(dialogView);

        // Aquí puedes agregar una lista o un RecyclerView para mostrar todas las misiones
        LinearLayout missionLayout = dialogView.findViewById(R.id.missionLayout);

        for (Mission mission : missions) {
            View missionItemView = inflater.inflate(R.layout.mission_item, null);
            TextView textViewIdVuelo = missionItemView.findViewById(R.id.textViewIdVuelo);
            TextView textViewTipo = missionItemView.findViewById(R.id.textViewTipo);
            TextView textViewMissionCode = missionItemView.findViewById(R.id.textViewMissionCode);

            textViewIdVuelo.setText("ID Vuelo: " + mission.getIdVuelo());
            textViewTipo.setText("Tipo: " + mission.getTipo());
            textViewMissionCode.setText("Mission Code: " + mission.getMissionCode());

            missionItemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Obtener el valor del "missionCode" del objeto seleccionado
                    Mission selectedMission = missions.get(missionLayout.indexOfChild(v));
                    missionCodeApp = selectedMission.getMissionCode();
                    System.out.println("HOla Mission.............. "+missionCodeApp);

                    // Cerrar el diálogo cuando el usuario selecciona una misión

                }
            });

            missionLayout.addView(missionItemView);
        }

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }





    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (gMap == null) {
            gMap = googleMap;
            setUpMap();
        }

        LatLng bogota = new LatLng(4.624335, -74.063644);
        gMap.addMarker(new MarkerOptions().position(bogota).title("Marker in Bogota"));
        gMap.moveCamera(CameraUpdateFactory.newLatLng(bogota));
    }

    private void showToast(final String toastMsg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();

            }
        });
    }


}
