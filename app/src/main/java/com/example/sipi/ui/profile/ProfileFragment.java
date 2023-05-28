package com.example.sipi.ui.profile;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.sipi.Login;
import com.example.sipi.MainActivity;
import com.example.sipi.databinding.FragmentProfileBinding;

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
import java.util.Objects;

public class ProfileFragment extends Fragment {

    SharedPreferences pref;
    String response;
    String token;
    private FragmentProfileBinding binding;

    private class ProfileThread implements Runnable{

        @Override
        public void run() {

            String string_request = "https://assistant.5pwjust.ru/api/users/me/";
            Looper.prepare();
            try {
                URL url = new URL(string_request);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setConnectTimeout(5000);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoInput(true);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);

                InputStream in = new BufferedInputStream(conn.getInputStream());
                response = IOUtils.toString(in, StandardCharsets.UTF_8);
                in.close();

                conn.disconnect();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ProfileViewModel profileViewModel =
                new ViewModelProvider(this).get(ProfileViewModel.class);

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        SharedPreferences pref = requireActivity().getSharedPreferences("prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        if (pref.contains("token")) {
            token = pref.getString("token", "");
            System.out.println(token);
        } else {
            token = requireActivity().getIntent().getStringExtra("token");
        }

        Thread myThread = new Thread(new ProfileThread());
        myThread.start();
        try {
            myThread.join();
        } catch (InterruptedException ignored) {
        }

        Button exit = binding.btnExit;
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.clear().apply();

                startActivity(new Intent(getActivity(), Login.class));
                requireActivity().finish();
            }
        });

        TextView username = binding.textUsername;
        TextView fullname = binding.textFullname;
        TextView cipher = binding.textCipher;
        TextView role = binding.textRole;

        try {
            JSONObject jsonObject = new JSONObject(response);
            username.setText(jsonObject.getString("username"));
            editor.putString("usename", jsonObject.getString("username")).apply();
            fullname.setText(jsonObject.getString("user_fullname"));
            cipher.setText(jsonObject.getString("personal_cipher"));
            switch (jsonObject.getString("role")) {
                case "1" -> role.setText("Обычный пользователь");
                case "2" -> role.setText("Модератор");
                case "3" -> role.setText("Администратор");
                default -> role.setText("Кажется мы не знаем кто вы");
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}