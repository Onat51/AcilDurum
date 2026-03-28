package com.example.emergencyapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class PreferenceManager {
    private SharedPreferences prefs;

    public PreferenceManager(Context context) {
        prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setUserId(String id) {
        prefs.edit().putString(Constants.PREF_USER_ID, id).apply();
    }

    public String getUserId() {
        return prefs.getString(Constants.PREF_USER_ID, null);
    }

    public void setUserName(String name) {
        prefs.edit().putString(Constants.PREF_USER_NAME, name).apply();
    }

    public String getUserName() {
        return prefs.getString(Constants.PREF_USER_NAME, null);
    }

    public void setSetupComplete(boolean complete) {
        prefs.edit().putBoolean(Constants.PREF_SETUP_COMPLETE, complete).apply();
    }

    public boolean isSetupComplete() {
        return prefs.getBoolean(Constants.PREF_SETUP_COMPLETE, false);
    }

    public void setFcmToken(String token) {
        prefs.edit().putString(Constants.PREF_FCM_TOKEN, token).apply();
    }

    public String getFcmToken() {
        return prefs.getString(Constants.PREF_FCM_TOKEN, null);
    }

    public void setAdmin(boolean isAdmin) {
        prefs.edit().putBoolean(Constants.PREF_IS_ADMIN, isAdmin).apply();
    }

    public boolean isAdmin() {
        return prefs.getBoolean(Constants.PREF_IS_ADMIN, false);
    }

    // Seçili kullanıcılar (acil durum gönderilecek/alınacak)
    public void setSelectedUsers(Set<String> userIds) {
        prefs.edit().putStringSet(Constants.PREF_SELECTED_USERS, userIds).apply();
    }

    public Set<String> getSelectedUsers() {
        return prefs.getStringSet(Constants.PREF_SELECTED_USERS, new HashSet<>());
    }

    public void addSelectedUser(String oderId) {
        Set<String> users = new HashSet<>(getSelectedUsers());
        users.add(oderId);
        setSelectedUsers(users);
    }

    public void removeSelectedUser(String oderId) {
        Set<String> users = new HashSet<>(getSelectedUsers());
        users.remove(oderId);
        setSelectedUsers(users);
    }

    public boolean isUserSelected(String oderId) {
        return getSelectedUsers().contains(oderId);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}