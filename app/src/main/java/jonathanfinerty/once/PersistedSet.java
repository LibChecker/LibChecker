package jonathanfinerty.once;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

class PersistedSet {

    private static final String STRING_SET_KEY = "PersistedSetValues";

    private final SharedPreferences preferences;
    private final Set<String> set;


    PersistedSet(Context context, String setName) {
        String preferencesName = "PersistedSet".concat(setName);
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE);
        set = preferences.getStringSet(STRING_SET_KEY, new HashSet<>());
    }

    void put(String tag) {
        set.add(tag);
        updatePreferences();
    }

    boolean contains(String tag) {
        return set.contains(tag);
    }

    void remove(String tag) {
        set.remove(tag);
        updatePreferences();
    }

    void clear() {
        set.clear();
        updatePreferences();
    }

    private void updatePreferences() {
        SharedPreferences.Editor edit = preferences.edit();

        edit.putStringSet(STRING_SET_KEY, set);
        edit.apply();
    }
}
