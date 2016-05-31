/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2016 vanita5 <mail@vanit.as>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2016 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vanita5.twittnuker.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import de.vanita5.twittnuker.BuildConfig;
import de.vanita5.twittnuker.annotation.Preference;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static de.vanita5.twittnuker.TwittnukerConstants.LOGTAG;

public class SharedPreferencesWrapper implements SharedPreferences {

    private final Context mContext;
    private final SharedPreferences mPreferences;
    private final HashMap<String, Preference> mMap;

    private SharedPreferencesWrapper(final Context context, SharedPreferences preferences, final Class<?> keysClass) {
        mContext = context;
        mPreferences = preferences;
        mMap = new HashMap<>();
        if (keysClass != null) {
            for (Field field : keysClass.getFields()) {
                final Preference preference = field.getAnnotation(Preference.class);
                if (preference == null) continue;
                try {
                    mMap.put((String) field.get(null), preference);
                } catch (Exception ignore) {
                }
            }
        }
    }

    @Override
    public boolean contains(final String key) {
        return mPreferences.contains(key);
    }

    @Override
    public SharedPreferences.Editor edit() {
        return mPreferences.edit();
    }

    @Override
    public Map<String, ?> getAll() {
        return mPreferences.getAll();
    }

    @Override
    public boolean getBoolean(final String key, final boolean defValue) {
        try {
            return mPreferences.getBoolean(key, defValue);
        } catch (final ClassCastException e) {
            if (BuildConfig.DEBUG) Log.w(LOGTAG, e);
            mPreferences.edit().remove(key).apply();
            return defValue;
        }
    }

    public boolean getBoolean(final String key) {
        return getBoolean(key, getDefaultBoolean(key));
    }

    private boolean getDefaultBoolean(String key) {
        final Preference annotation = mMap.get(key);
        if (annotation == null || !annotation.hasDefault()) return false;
        final int resId = annotation.defaultResource();
        if (resId != 0) return mContext.getResources().getBoolean(resId);
        return annotation.defaultBoolean();
    }

    private int getDefaultInt(String key) {
        final Preference annotation = mMap.get(key);
        if (annotation == null || !annotation.hasDefault()) return 0;
        final int resId = annotation.defaultResource();
        if (resId != 0) return mContext.getResources().getInteger(resId);
        return annotation.defaultInt();
    }

    @Override
    public float getFloat(final String key, final float defValue) {
        try {
            return mPreferences.getFloat(key, defValue);
        } catch (final ClassCastException e) {
            if (BuildConfig.DEBUG) Log.w(LOGTAG, e);
            mPreferences.edit().remove(key).apply();
            return defValue;
        }
    }

    public int getInt(final String key) {
        return getInt(key, getDefaultInt(key));
    }

    @Override
    public int getInt(final String key, final int defValue) {
        try {
            return mPreferences.getInt(key, defValue);
        } catch (final ClassCastException e) {
            if (BuildConfig.DEBUG) Log.w(LOGTAG, e);
            mPreferences.edit().remove(key).apply();
            return defValue;
        }
    }

    @Override
    public long getLong(final String key, final long defValue) {
        try {
            return mPreferences.getLong(key, defValue);
        } catch (final ClassCastException e) {
            if (BuildConfig.DEBUG) Log.w(LOGTAG, e);
            mPreferences.edit().remove(key).apply();
            return defValue;
        }
    }

    public SharedPreferences getSharedPreferences() {
        return mPreferences;
    }

    @Override
    public String getString(final String key, final String defValue) {
        try {
            return mPreferences.getString(key, defValue);
        } catch (final ClassCastException e) {
            if (BuildConfig.DEBUG) Log.w(LOGTAG, e);
            mPreferences.edit().remove(key).apply();
            return defValue;
        }
    }

    @Override
    public Set<String> getStringSet(final String key, final Set<String> defValue) {
        try {
            return mPreferences.getStringSet(key, defValue);
        } catch (final ClassCastException e) {
            if (BuildConfig.DEBUG) Log.w(LOGTAG, e);
            mPreferences.edit().remove(key).apply();
            return defValue;
        }
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(final OnSharedPreferenceChangeListener listener) {
        mPreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(final OnSharedPreferenceChangeListener listener) {
        mPreferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public static SharedPreferencesWrapper getInstance(final Context context, final String name, final int mode) {
        return getInstance(context, name, mode, null);
    }

    @NonNull
    public static SharedPreferencesWrapper getInstance(final Context context, final String name, final int mode,
                                                       final Class<?> keysClass) {
        final Context app = context.getApplicationContext();
        final SharedPreferences prefs = app.getSharedPreferences(name, mode);
        if (prefs == null) throw new NullPointerException();
        return new SharedPreferencesWrapper(app, prefs, keysClass);
    }

}