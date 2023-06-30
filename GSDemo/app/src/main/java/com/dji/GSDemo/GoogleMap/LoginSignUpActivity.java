package com.dji.GSDemo.GoogleMap;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import dji.sdk.sdkmanager.DJISDKManager;

public class LoginSignUpActivity extends Activity implements View.OnClickListener {

    private static final String TAG = LoginSignUpActivity.class.getName();


    //Atributos defecto

    private TextView mTextConnectionStatus;
    private TextView mTextProduct;
    private TextView mVersionTv;


    private Button mBtnLogin;
    private Button mBtnSignUp;





    private ImageView correctImg;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initUI();

        // Register the broadcast receiver for receiving the device connection's changes.
        IntentFilter filter = new IntentFilter();
        filter.addAction(DJIDemoApplication.FLAG_CONNECTION_CHANGE);

    }


    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    public void onReturn(View view){
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
    }

    private void initUI() {
        mTextConnectionStatus = (TextView) findViewById(R.id.text_connection_status);
        mTextProduct = (TextView) findViewById(R.id.text_product_info);

        mBtnLogin = (Button) findViewById(R.id.btn_signIn) ;
        mBtnLogin.setOnClickListener(this);

        mBtnSignUp = (Button) findViewById(R.id.btn_signUp) ;
        mBtnSignUp.setOnClickListener(this);


        mVersionTv = (TextView) findViewById(R.id.textView2);
        mVersionTv.setText(getResources().getString(R.string.sdk_version, DJISDKManager.getInstance().getSDKVersion()));

        correctImg = (ImageView) findViewById(R.id.imageView);
    }





    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.btn_signIn: {
                Intent intent = new Intent(LoginSignUpActivity.this, LoginActivity.class);
                startActivity(intent);

                break;
            }

            case R.id.btn_signUp: {
                Intent intent = new Intent(LoginSignUpActivity.this, RegistrationActivity.class);
                startActivity(intent);

                break;
            }

            default:
                break;
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

}

