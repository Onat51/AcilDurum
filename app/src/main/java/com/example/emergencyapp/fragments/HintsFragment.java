package com.example.emergencyapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emergencyapp.FirebaseManager;
import com.example.emergencyapp.R;
import com.example.emergencyapp.adapters.MessageAdapter;
import com.example.emergencyapp.models.Message;
import com.example.emergencyapp.utils.PreferenceManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HintsFragment extends Fragment {
    private RecyclerView rvHints;
    private TextView tvNoHints;

    private String emergencyId;
    private PreferenceManager prefManager;
    private MessageAdapter adapter;
    private List<Message> hints = new ArrayList<>();

    public static HintsFragment newInstance(String emergencyId) {
        HintsFragment fragment = new HintsFragment();
        Bundle args = new Bundle();
        args.putString("emergency_id", emergencyId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            emergencyId = getArguments().getString("emergency_id");
        }
        prefManager = new PreferenceManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hints, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvHints = view.findViewById(R.id.rvHints);
        tvNoHints = view.findViewById(R.id.tvNoHints);

        // MessageAdapter kullan (HintAdapter yerine)
        adapter = new MessageAdapter(hints, prefManager.getUserId());
        rvHints.setLayoutManager(new LinearLayoutManager(getContext()));
        rvHints.setAdapter(adapter);

        listenToHints();
    }

    private void listenToHints() {
        FirebaseManager.getInstance().listenToHints(emergencyId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                hints.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Message hint = child.getValue(Message.class);
                    if (hint != null) {
                        hints.add(hint);
                    }
                }
                adapter.notifyDataSetChanged();

                tvNoHints.setVisibility(hints.isEmpty() ? View.VISIBLE : View.GONE);
                rvHints.setVisibility(hints.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}