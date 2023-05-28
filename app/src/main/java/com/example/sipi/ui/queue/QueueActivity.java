package com.example.sipi.ui.queue;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sipi.MainActivity;
import com.example.sipi.R;
import com.example.sipi.databinding.FragmentProfileBinding;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class QueueActivity extends AppCompatActivity {

    String response;
    String token;
    private FragmentProfileBinding binding;

    private class FilteredQueueThread implements Runnable{

        @Override
        public void run() {
            System.out.println(getIntent().getStringExtra("subject"));
            String string_request = "https://assistant.5pwjust.ru/api/queue/filtered?subject=" + getIntent().getStringExtra("subject");
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

    static class MyAdapter extends RecyclerView.Adapter<QueueActivity.MyAdapter.MyViewHolder> {

        private List<Student> itemList;

        public MyAdapter(List<Student> itemList) {
            this.itemList = itemList;
        }

        @NonNull
        @Override
        public QueueActivity.MyAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.student_item, parent, false);
            return new QueueActivity.MyAdapter.MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull QueueActivity.MyAdapter.MyViewHolder holder, int position) {
            Student item = itemList.get(position);

            String buff_position = String.valueOf(position + 1);
            holder.position.setText(holder.date.getContext().getString(R.string.title_position) + " " + buff_position);
            holder.name.setText(item.fullname);
            holder.date.setText(item.date);
        }

        @Override
        public int getItemCount() {
            return itemList.size();
        }

        public static class MyViewHolder extends RecyclerView.ViewHolder {
            public TextView position;
            public TextView name;
            public TextView date;
            public MyViewHolder(View view) {
                super(view);
                position = view.findViewById(R.id.student_position);
                name = view.findViewById(R.id.student_name);
                date = view.findViewById(R.id.student_date);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getIntent().getStringExtra("name").toUpperCase() + " - " + getIntent().getStringExtra("is_open"));

        SharedPreferences pref = getSharedPreferences("prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        if (pref.contains("token")) {
            token = pref.getString("token", "");
            System.out.println(token);
        } else {
            token = getIntent().getStringExtra("token");
        }

        Thread myThread = new Thread(new QueueActivity.FilteredQueueThread());
        myThread.start();
        try {
            myThread.join();
        } catch (InterruptedException ignored) {
        }

        ArrayList<String> usernames = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray("queue_persons");

            List<Student> arr = new ArrayList<>();

            for(int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject2 = jsonArray.getJSONObject(i);
                usernames.add(jsonObject2.getString("username"));
                String name = jsonObject2.getString("user_fullname");
                String dateString = jsonObject2.getString("timestamp");
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",  Locale.getDefault());
                format.setTimeZone(TimeZone.getTimeZone("GMT-3"));
                Date date = format.parse(dateString);

                arr.add(new Student(name, date.toString()));
            }

            RecyclerView recyclerView = findViewById(R.id.queue_recycler);
            MyAdapter adapter = new MyAdapter(arr);
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        } catch (JSONException | ParseException e) {
            throw new RuntimeException(e);
        }

        Button btn_in = findViewById(R.id.btn_queue_in);
        Button btn_out = findViewById(R.id.btn_queue_out);
        Button btn_back = findViewById(R.id.btn_back);

        String username = pref.getString("usename", "");

        System.out.println(usernames);
        System.out.println(username);

        if (usernames.contains(username)) {
            btn_in.setEnabled(false);
        } else {
            btn_out.setEnabled(false);
        }

        btn_in.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                class QueueInThread implements Runnable{

                    @Override
                    public void run() {
                        System.out.println(getIntent().getStringExtra("subject"));
                        String string_request = "https://assistant.5pwjust.ru/api/queue/";

                        JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.put("subject", getIntent().getStringExtra("subject"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Looper.prepare();
                        try {
                            URL url = new URL(string_request);
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                            conn.setConnectTimeout(5000);
                            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                            conn.setDoInput(true);
                            conn.setDoOutput(true);
                            conn.setRequestMethod("POST");
                            conn.setRequestProperty("Authorization", "Bearer " + token);

                            OutputStream os = conn.getOutputStream();
                            os.write(jsonObject.toString().getBytes(StandardCharsets.UTF_8));
                            os.close();

                            InputStream in = new BufferedInputStream(conn.getInputStream());
                            response = IOUtils.toString(in, StandardCharsets.UTF_8);
                            in.close();

                            conn.disconnect();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                Thread myThread = new Thread(new QueueInThread());
                myThread.start();
                try {
                    myThread.join();
                } catch (InterruptedException ignored) {
                }

                recreate();

            }
        });

        btn_out.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                class QueueOutThread implements Runnable{

                    @Override
                    public void run() {
                        System.out.println(getIntent().getStringExtra("subject"));
                        String string_request = "https://assistant.5pwjust.ru/api/queue/" + getIntent().getStringExtra("subject") + "/";

                        Looper.prepare();
                        try {
                            URL url = new URL(string_request);
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                            conn.setRequestMethod("DELETE");
                            conn.setRequestProperty("Authorization", "Bearer " + token);
                            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                            conn.setConnectTimeout(5000);
                            conn.setDoInput(true);

                            InputStream in = new BufferedInputStream(conn.getInputStream());
                            response = IOUtils.toString(in, StandardCharsets.UTF_8);
                            in.close();

                            conn.disconnect();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                Thread myThread = new Thread(new QueueOutThread());
                myThread.start();
                try {
                    myThread.join();
                } catch (InterruptedException ignored) {
                }

                recreate();
            }
        });

        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(QueueActivity.this, MainActivity.class));
                finish();
            }
        });

    }


}