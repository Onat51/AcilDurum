package com.example.emergencyapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.emergencyapp.FirebaseManager;
import com.example.emergencyapp.R;
import com.example.emergencyapp.models.CustomEmergencyCode;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class EnterCodeActivity extends AppCompatActivity {

    private EditText etCode;
    private Button btnSearch;
    private LinearLayout llResults;
    private TextView tvEmpty;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_code);

        etCode = findViewById(R.id.etCode);
        btnSearch = findViewById(R.id.btnSearch);
        llResults = findViewById(R.id.llResults);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        btnSearch.setOnClickListener(v -> {
            String code = etCode.getText().toString().trim();
            if (code.isEmpty()) {
                etCode.setError("Kod boş olamaz");
                return;
            }
            lookupCode(code);
        });
    }

    private void lookupCode(String code) {
        btnSearch.setEnabled(false);
        btnSearch.setText("Aranıyor...");

        FirebaseManager.getInstance().lookupCode(code, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                btnSearch.setEnabled(true);
                btnSearch.setText("Ara");
                llResults.removeAllViews();

                if (!snapshot.exists()) {
                    tvEmpty.setText("\"" + code + "\" kodu bulunamadı.");
                    tvEmpty.setVisibility(android.view.View.VISIBLE);
                    return;
                }

                tvEmpty.setVisibility(android.view.View.GONE);
                List<CustomEmergencyCode> found = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    CustomEmergencyCode c = child.getValue(CustomEmergencyCode.class);
                    if (c != null && c.isActive()) found.add(c);
                }

                if (found.isEmpty()) {
                    tvEmpty.setText("Bu kod aktif değil.");
                    tvEmpty.setVisibility(android.view.View.VISIBLE);
                    return;
                }

                for (CustomEmergencyCode c : found) {
                    addResultCard(c);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                btnSearch.setEnabled(true);
                btnSearch.setText("Ara");
                Toast.makeText(EnterCodeActivity.this, "Arama hatası", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addResultCard(CustomEmergencyCode code) {
        android.view.View card = getLayoutInflater().inflate(R.layout.item_custom_code, llResults, false);

        TextView tvTitle = card.findViewById(R.id.tvTitle);
        TextView tvDesc = card.findViewById(R.id.tvDesc);
        Button btnUse = card.findViewById(R.id.btnUse);

        tvTitle.setText("⚡ " + code.getTitle());
        tvDesc.setText(code.getDescription() != null ? code.getDescription() : "");

        btnUse.setOnClickListener(v -> {
            // Return to MainActivity with this custom code
            Intent result = new Intent();
            result.putExtra("custom_title", code.getTitle());
            result.putExtra("custom_description", code.getDescription());
            result.putExtra("custom_code_id", code.getId());
            setResult(RESULT_OK, result);
            finish();
        });

        llResults.addView(card);
    }
}