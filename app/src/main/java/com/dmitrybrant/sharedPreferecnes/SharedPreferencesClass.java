package com.dmitrybrant.sharedPreferecnes;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by dharamveer on 5/4/18.
 */

public class SharedPreferencesClass {


     SharedPreferences sharedPreferences;

     Context context;
    private  String session_key;

    public SharedPreferencesClass(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences("3dKey", Context.MODE_PRIVATE);

    }

    public  String getSession_key() {
        session_key = sharedPreferences.getString("session_key",session_key);

        return session_key;
    }

    public  void setSession_key(String session_key) {
        session_key = session_key;
        sharedPreferences.edit().putString("session_key",session_key).commit();

    }
}
