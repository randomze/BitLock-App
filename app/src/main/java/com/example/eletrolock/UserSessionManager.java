package com.example.eletrolock;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

public class UserSessionManager {

    private SharedPreferences preferences;

    private SharedPreferences.Editor editor;

    private Context _context;

    private int PRIVATE_MODE = 0;

    private String preferencesName = "SessionPreferences";

    public UserSessionManager(Context context) {
        this._context = context;

        preferences = _context.getSharedPreferences(preferencesName, PRIVATE_MODE);
        editor = preferences.edit();
    }

    public void setToken(String token) {
        editor.putString("token", token);

        editor.commit();
    }

    public void setMasterID(String masterID) {
        editor.putString("masterID", masterID);

        editor.commit();
    }

    public String getMasterID() {
        return preferences.getString("masterID", null);
    }

    public String getToken() {
        return preferences.getString("token", null);
    }

    public HashMap<String, String> getUserLogin() {
        HashMap<String, String> user = new HashMap<String, String>();

        user.put("email", preferences.getString("email", null));
        user.put("password", preferences.getString("password", null));

        return user;
    }

    public String getUnique() {
        return preferences.getString("unique", null);
    }

    public void login(String email, String password, String unique) {
        editor.putString("email", email);
        editor.putString("password", password);
        editor.putString("unique", unique);

        editor.commit();
    }

    public boolean checkIfLogin() {

        return preferences.contains("email") && preferences.contains("password");
    }
}
