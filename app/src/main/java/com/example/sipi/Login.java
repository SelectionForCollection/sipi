package com.example.sipi;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;


public class Login extends AppCompatActivity {

    EditText username;
    EditText password;
    String response;
    String token;
    SharedPreferences pref;

    private class MyLoginThread implements Runnable{

        @Override
        public void run() {
            // Create URL
            String string_request = "https://assistant.5pwjust.ru/api/auth/jwt/create/";
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("username", username.getText().toString());
                jsonObject.put("password", password.getText().toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Looper.prepare();
            try {
                URL url = new URL(string_request);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setConnectTimeout(5000);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(jsonObject.toString().getBytes(StandardCharsets.UTF_8));
                os.close();

                InputStream in = conn.getInputStream();
                response = IOUtils.toString(in, StandardCharsets.UTF_8);
                in.close();

                conn.disconnect();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        pref = getSharedPreferences("prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
//        editor.clear().apply();

        if (pref.contains("token")) {
            token = pref.getString("token", "");
            System.out.println(token);
            startActivity(new Intent(Login.this, MainActivity.class).putExtra("token", token));
            finish();
        } else {
            Toast.makeText(this, "Кажется мы вас не помним", Toast.LENGTH_SHORT).show();
        }

        username = findViewById(R.id.edit_login);
        password = findViewById(R.id.edit_password);

        Button btn_login = findViewById(R.id.btn_login);
        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Thread myThread = new Thread(new MyLoginThread());
                myThread.start();
                try {
                    myThread.join();
                } catch (InterruptedException ignored) {
                }

                token = response.split(",")[1].split(":")[1];
                token = token.substring(1, token.length() - 2);

                CheckBox checkBox = findViewById(R.id.checkbox);

                if (checkBox.isChecked()) {
                    editor.putString("token", token).apply(); // YbuqstiNucIM
                    startActivity(new Intent(Login.this, MainActivity.class));
                } else {
                    startActivity(new Intent(Login.this, MainActivity.class).putExtra("token", token));
                }
                finish();
            }
        });


    }
}