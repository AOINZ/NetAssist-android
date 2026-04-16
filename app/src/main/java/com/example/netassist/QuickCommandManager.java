package com.example.netassist;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class QuickCommandManager {
    private static final String PREF_NAME = "QuickCommands";
    private static final String KEY_COMMANDS = "commands";
    
    private SharedPreferences sharedPreferences;
    private Gson gson;
    
    public QuickCommandManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }
    
    public void saveCommands(List<QuickCommand> commands) {
        String json = gson.toJson(commands);
        sharedPreferences.edit().putString(KEY_COMMANDS, json).apply();
    }
    
    public List<QuickCommand> loadCommands() {
        String json = sharedPreferences.getString(KEY_COMMANDS, null);
        if (json == null) {
            return new ArrayList<>();
        }
        
        Type type = new TypeToken<List<QuickCommand>>(){}.getType();
        return gson.fromJson(json, type);
    }
}
