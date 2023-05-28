package com.example.sipi.ui.queue;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sipi.R;
import com.example.sipi.databinding.FragmentQueueBinding;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.json.*;

public class QueueFragment extends Fragment {

    String response;
    String token;
    private FragmentQueueBinding binding;

    private class QueueThread implements Runnable{

        @Override
        public void run() {

            String string_request = "https://assistant.5pwjust.ru/api/subjects/";
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

    static class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

        private List<Queue> itemList;

        public MyAdapter(List<Queue> itemList) {
            this.itemList = itemList;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.queue_item, parent, false);
            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            Queue item = itemList.get(position);
            holder.name.setText(item.name);
            holder.status.setText(item.status);
            holder.peoples.setText(item.peoples);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(view.getContext(), QueueActivity.class);
                    intent.putExtra("subject", item.slug);
                    intent.putExtra("is_open", item.status);
                    intent.putExtra("name", item.name);
                    view.getContext().startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return itemList.size();
        }

        public static class MyViewHolder extends RecyclerView.ViewHolder {
            public TextView name;
            public TextView status;
            public TextView peoples;
            public MyViewHolder(View view) {
                super(view);
                name = view.findViewById(R.id.queue_name);
                status = view.findViewById(R.id.queue_status);
                peoples = view.findViewById(R.id.queue_peoples);
            }
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        QueueViewModel queueViewModel =
                new ViewModelProvider(this).get(QueueViewModel.class);

        binding = FragmentQueueBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        SharedPreferences pref = requireActivity().getSharedPreferences("prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        if (pref.contains("token")) {
            token = pref.getString("token", "");
            System.out.println(token);
        } else {
            token = requireActivity().getIntent().getStringExtra("token");
        }

        Thread myThread = new Thread(new QueueFragment.QueueThread());
        myThread.start();
        try {
            myThread.join();
        } catch (InterruptedException ignored) {
        }

        try {
            JSONArray jsonArray = new JSONArray(response);

            List<Queue> arr = new ArrayList<>();
            for(int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String name = jsonObject.getString("title");
                String status = jsonObject.getString("queue_is_open");
                String peoples = jsonObject.getString("count_in_queue");
                String slug = jsonObject.getString("slug");
                arr.add(new Queue(name, status, peoples, slug));
            }

            RecyclerView recyclerView = binding.recyclerView;
            MyAdapter adapter = new MyAdapter(arr);
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
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