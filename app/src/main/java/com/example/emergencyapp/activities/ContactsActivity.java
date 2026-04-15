package com.example.emergencyapp.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emergencyapp.FirebaseManager;
import com.example.emergencyapp.R;
import com.example.emergencyapp.models.User;
import com.example.emergencyapp.utils.Constants;
import com.example.emergencyapp.utils.PreferenceManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ContactsActivity extends AppCompatActivity {

    private EditText etSearch;
    private RecyclerView rvContacts;
    private RecyclerView rvSelected;
    private LinearLayout tvNoResults;
    private TextView tvSelectedCount;
    private ImageButton btnBack;

    private PreferenceManager prefManager;
    private List<User> allUsers = new ArrayList<>();
    private List<User> filteredUsers = new ArrayList<>();
    private List<User> selectedUsers = new ArrayList<>();

    private ContactAdapter contactAdapter;
    private SelectedContactAdapter selectedAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        prefManager = new PreferenceManager(this);

        initViews();
        loadAllUsers();
    }

    private void initViews() {
        etSearch = findViewById(R.id.etSearch);
        rvContacts = findViewById(R.id.rvContacts);
        rvSelected = findViewById(R.id.rvSelected);
        tvNoResults = findViewById(R.id.tvNoResults);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Contact list (search results)
        contactAdapter = new ContactAdapter(filteredUsers, user -> toggleContact(user));
        rvContacts.setLayoutManager(new LinearLayoutManager(this));
        rvContacts.setAdapter(contactAdapter);

        // Selected contacts list
        selectedAdapter = new SelectedContactAdapter(selectedUsers, user -> removeContact(user));
        rvSelected.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSelected.setAdapter(selectedAdapter);

        updateSelectedCount();
    }

    private void loadAllUsers() {
        String myId = prefManager.getUserId();
        Set<String> savedIds = prefManager.getSelectedUsers();

        FirebaseManager.getInstance().listenToUsers(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allUsers.clear();
                selectedUsers.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    User user = child.getValue(User.class);
                    if (user != null && !user.getId().equals(myId)) {
                        allUsers.add(user);
                        if (savedIds.contains(user.getId())) {
                            selectedUsers.add(user);
                        }
                    }
                }
                // Don't show list until searched
                filteredUsers.clear();
                contactAdapter.notifyDataSetChanged();
                selectedAdapter.notifyDataSetChanged();
                updateSelectedCount();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ContactsActivity.this, "Kullanıcılar yüklenemedi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterUsers(String query) {
        filteredUsers.clear();
        if (query.length() < 1) {
            tvNoResults.setVisibility(View.VISIBLE);
            rvContacts.setVisibility(View.GONE);
            contactAdapter.notifyDataSetChanged();
            return;
        }

        String lower = query.toLowerCase().trim();
        boolean isAdmin = prefManager.isAdmin();

        for (User u : allUsers) {
            // Admin: show all; others: only matching names
            if (isAdmin || (u.getName() != null && u.getName().toLowerCase().contains(lower))) {
                filteredUsers.add(u);
            }
        }

        tvNoResults.setVisibility(filteredUsers.isEmpty() ? View.VISIBLE : View.GONE);
        rvContacts.setVisibility(filteredUsers.isEmpty() ? View.GONE : View.VISIBLE);
        contactAdapter.notifyDataSetChanged();
    }

    private void toggleContact(User user) {
        boolean isSelected = prefManager.isUserSelected(user.getId());
        if (isSelected) {
            prefManager.removeSelectedUser(user.getId());
            selectedUsers.removeIf(u -> u.getId().equals(user.getId()));
        } else {
            prefManager.addSelectedUser(user.getId());
            selectedUsers.add(user);
        }
        selectedAdapter.notifyDataSetChanged();
        contactAdapter.notifyDataSetChanged();
        updateSelectedCount();
    }

    private void removeContact(User user) {
        prefManager.removeSelectedUser(user.getId());
        selectedUsers.removeIf(u -> u.getId().equals(user.getId()));
        selectedAdapter.notifyDataSetChanged();
        contactAdapter.notifyDataSetChanged();
        updateSelectedCount();
    }

    private void updateSelectedCount() {
        int count = prefManager.getSelectedUsers().size();
        tvSelectedCount.setText("Seçili kişiler (" + count + ")");
    }

    // ─── Inner Adapter: Search results ─────────────────────────────────────────

    static class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.VH> {
        interface OnClickListener { void onClick(User user); }
        private List<User> list;
        private OnClickListener listener;
        ContactAdapter(List<User> list, OnClickListener listener) {
            this.list = list; this.listener = listener;
        }
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_contact, parent, false);
            return new VH(v);
        }
        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            User u = list.get(position);
            holder.bind(u, listener);
        }
        @Override public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvStatus;
            View checkIndicator;
            VH(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvName);
                tvStatus = v.findViewById(R.id.tvStatus);
                checkIndicator = v.findViewById(R.id.checkIndicator);
            }
            void bind(User u, OnClickListener l) {
                tvName.setText(u.getName());
                boolean sel = ((ContactsActivity) itemView.getContext())
                        .prefManager.isUserSelected(u.getId());
                checkIndicator.setVisibility(sel ? View.VISIBLE : View.GONE);
                tvStatus.setText(sel ? "✓ Seçili" : "Ekle");
                itemView.setOnClickListener(v -> l.onClick(u));
            }
        }
    }

    // ─── Inner Adapter: Selected chips ─────────────────────────────────────────

    static class SelectedContactAdapter extends RecyclerView.Adapter<SelectedContactAdapter.VH> {
        interface OnRemove { void onRemove(User user); }
        private List<User> list;
        private OnRemove listener;
        SelectedContactAdapter(List<User> list, OnRemove listener) {
            this.list = list; this.listener = listener;
        }
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_contact_chip, parent, false);
            return new VH(v);
        }
        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            User u = list.get(position);
            holder.tvName.setText(u.getName());
            holder.itemView.setOnClickListener(v -> listener.onRemove(u));
        }
        @Override public int getItemCount() { return list.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView tvName;
            VH(View v) { super(v); tvName = v.findViewById(R.id.tvName); }
        }
    }
}