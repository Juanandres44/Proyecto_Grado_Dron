package com.dji.GSDemo.GoogleMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import dji.sdk.sdkmanager.DJISDKManager;
import dji.thirdparty.afinal.core.AsyncTask;

public class LoginActivity extends Activity implements View.OnClickListener {

    private static final String TAG = LoginActivity.class.getName();


    //Atributos defecto

    private TextView mTextConnectionStatus;
    private TextView mTextProduct;
    private TextView mVersionTv;


    private Button mBtnLogin;


    private EditText mEmail;

    private EditText mPassword;



    private ImageView correctImg;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_info);

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

        mEmail = (EditText) findViewById(R.id.input_email);
        mPassword = (EditText) findViewById(R.id.input_password);
        mBtnLogin = (Button) findViewById(R.id.btn_signInApp) ;
        mBtnLogin.setOnClickListener(this);


        mVersionTv = (TextView) findViewById(R.id.textView2);
        mVersionTv.setText(getResources().getString(R.string.sdk_version, DJISDKManager.getInstance().getSDKVersion()));

        correctImg = (ImageView) findViewById(R.id.imageView);
    }





    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.btn_signInApp: {
                String email = mEmail.getText().toString();
                String password = mPassword.getText().toString();

                // Construye el objeto JSON con los datos
                JSONObject json = new JSONObject();
                try {
                    json.put("email", email);
                    json.put("password", password);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                SharedPreferences sharedPreferences = getSharedPreferences("Cache", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("PASSWORD", password);
                editor.apply();


                // Llama al método para enviar la solicitud HTTP POST al endpoint
                Integer i=0;
                try {
                    i = Integer.valueOf(new LoginActivity.PostRequestAsyncTask().execute(json).get().toString());
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    // Manejar las excepciones según tus necesidades
                }

                if(i==200){
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                }

                break;
            }
            default:
                break;
        }
    }

    private class PostRequestAsyncTask extends AsyncTask<JSONObject, Void, Integer> {
        @Override
        protected Integer doInBackground(JSONObject... jsonObjects) {
            JSONObject json = jsonObjects[0];
            try {
                URL url = new URL("http://3.208.19.176:80/api/usuario/login");

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(json.toString().getBytes());
                outputStream.flush();
                outputStream.close();

                if(connection.getResponseCode()==200){
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
                    String name = json1.getString("name");

                    SharedPreferences sharedPreferences = getSharedPreferences("Cache", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("RESPONSE", response.toString());
                    editor.putInt("IDUSER",id);
                    editor.putString("NAMEUSER",name);
                    editor.apply();

                    // Recuperar el valor almacenado en caché desde SharedPreferences
                    sharedPreferences = getSharedPreferences("Cache", Context.MODE_PRIVATE);
                    String cachedValueresponse = sharedPreferences.getString("RESPONSE","");
                    int cachedValueid = sharedPreferences.getInt("IDUSER",0);
                    String cachedValuename = sharedPreferences.getString("NAMEUSER","");

                    System.out.println("IDDDDDDD"+cachedValueid);

                }

                return connection.getResponseCode();
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void onPostExecute(Integer responseCode) {
            if (responseCode == HttpURLConnection.HTTP_OK) {
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

}

