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

package de.vanita5.twittnuker.app;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.ActionBarContextView;

import com.afollestad.appthemeengine.ATE;
import com.afollestad.appthemeengine.Config;
import com.pnikosis.materialishprogress.ProgressWheel;

import org.apache.commons.lang3.ArrayUtils;
import de.vanita5.twittnuker.BuildConfig;
import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.activity.AssistLauncherActivity;
import de.vanita5.twittnuker.activity.MainActivity;
import de.vanita5.twittnuker.service.RefreshService;
import de.vanita5.twittnuker.util.BugReporter;
import de.vanita5.twittnuker.util.DebugModeUtils;
import de.vanita5.twittnuker.util.ExternalThemeManager;
import de.vanita5.twittnuker.util.HttpClientFactory;
import de.vanita5.twittnuker.util.StrictModeUtils;
import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.util.TwidereBugReporter;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.util.content.TwidereSQLiteOpenHelper;
import de.vanita5.twittnuker.util.dagger.ApplicationModule;
import de.vanita5.twittnuker.util.dagger.DependencyHolder;
import de.vanita5.twittnuker.util.net.TwidereDns;
import de.vanita5.twittnuker.util.theme.ActionBarContextViewViewProcessor;
import de.vanita5.twittnuker.util.theme.ExtendedSwipeRefreshLayoutViewProcessor;
import de.vanita5.twittnuker.util.theme.FloatingActionButtonViewProcessor;
import de.vanita5.twittnuker.util.theme.ProgressWheelViewProcessor;
import de.vanita5.twittnuker.util.theme.TabPagerIndicatorViewProcessor;
import de.vanita5.twittnuker.util.theme.TimelineContentTextViewViewProcessor;
import de.vanita5.twittnuker.view.TabPagerIndicator;
import de.vanita5.twittnuker.view.TimelineContentTextView;
import de.vanita5.twittnuker.view.ExtendedSwipeRefreshLayout;

public class TwittnukerApplication extends Application implements Constants,
        OnSharedPreferenceChangeListener {

    private static final String KEY_KEYBOARD_SHORTCUT_INITIALIZED = "keyboard_shortcut_initialized";
    private static TwittnukerApplication sInstance;

    private Handler mHandler;
    private SharedPreferences mPreferences;
    private SQLiteOpenHelper mSQLiteOpenHelper;
    private SQLiteDatabase mDatabase;

    private ApplicationModule mApplicationModule;

    @NonNull
    public static TwittnukerApplication getInstance(@NonNull final Context context) {
        return (TwittnukerApplication) context.getApplicationContext();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        //MultiDex.install(this);
    }

    public Handler getHandler() {
        return mHandler;
    }

    public void initKeyboardShortcuts() {
        final SharedPreferences preferences = getSharedPreferences();
        if (!preferences.getBoolean(KEY_KEYBOARD_SHORTCUT_INITIALIZED, false)) {
//            getApplicationModule().getKeyboardShortcutsHandler().reset();
            preferences.edit().putBoolean(KEY_KEYBOARD_SHORTCUT_INITIALIZED, true).apply();
        }
    }


    public SQLiteDatabase getSQLiteDatabase() {
        if (mDatabase != null) return mDatabase;
        StrictModeUtils.checkDiskIO();
        return mDatabase = getSQLiteOpenHelper().getWritableDatabase();
    }

    public SQLiteOpenHelper getSQLiteOpenHelper() {
        if (mSQLiteOpenHelper != null) return mSQLiteOpenHelper;
        return mSQLiteOpenHelper = new TwidereSQLiteOpenHelper(this, DATABASES_NAME, DATABASES_VERSION);
    }

    @Override
    public void onCreate() {
        sInstance = this;
        if (BuildConfig.DEBUG) {
            StrictModeUtils.detectAllVmPolicy();
        }
        ATE.registerViewProcessor(TabPagerIndicator.class, new TabPagerIndicatorViewProcessor());
        ATE.registerViewProcessor(FloatingActionButton.class, new FloatingActionButtonViewProcessor());
        ATE.registerViewProcessor(ActionBarContextView.class, new ActionBarContextViewViewProcessor());
        ATE.registerViewProcessor(ExtendedSwipeRefreshLayout.class, new ExtendedSwipeRefreshLayoutViewProcessor());
        ATE.registerViewProcessor(TimelineContentTextView.class, new TimelineContentTextViewViewProcessor());
        ATE.registerViewProcessor(ProgressWheel.class, new ProgressWheelViewProcessor());
        final SharedPreferences preferences = getSharedPreferences();
        if (!ATE.config(this, null).isConfigured()) {
            final int themeColor = preferences.getInt(KEY_THEME_COLOR, ContextCompat.getColor(this,
                    R.color.branding_color));
            final int actionBarColor = preferences.getInt(KEY_ACTION_BAR_COLOR, ContextCompat.getColor(this,
                    R.color.material_dark));
            ATE.config(this, VALUE_THEME_NAME_LIGHT)
                    .primaryColor(themeColor)
                    .accentColor(themeColor)
                    .coloredActionBar(true)
                    .commit();
            ATE.config(this, VALUE_THEME_NAME_DARK)
                    .accentColor(themeColor)
                    .coloredActionBar(false)
                    .commit();
        }
        resetTheme(preferences);
        super.onCreate();
        initializeAsyncTask();
        initDebugMode();
        initBugReport();
        mHandler = new Handler();

        final PackageManager pm = getPackageManager();
        final ComponentName main = new ComponentName(this, MainActivity.class);
        pm.setComponentEnabledSetting(main, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
        if (!Utils.isComposeNowSupported(this)) {
            final ComponentName assist = new ComponentName(this, AssistLauncherActivity.class);
            pm.setComponentEnabledSetting(assist, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }
        Utils.startRefreshServiceIfNeeded(this);

        DependencyHolder holder = DependencyHolder.get(this);
        registerActivityLifecycleCallbacks(holder.getActivityTracker());

        final IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final int uid = intent.getIntExtra(Intent.EXTRA_UID, -1);
                final String[] packages = getPackageManager().getPackagesForUid(uid);
                DependencyHolder holder = DependencyHolder.get(context);
                final ExternalThemeManager manager = holder.getExternalThemeManager();
                if (ArrayUtils.contains(packages, manager.getEmojiPackageName())) {
                    manager.reloadEmojiPreferences();
                }
            }
        }, packageFilter);
    }

    private void initDebugMode() {
        DebugModeUtils.initForApplication(this);
    }

    private void initBugReport() {
        final SharedPreferences preferences = getSharedPreferences();
        if (!preferences.getBoolean(KEY_BUG_REPORTS, true)) return;
        BugReporter.setImplementation(new TwidereBugReporter());
        BugReporter.init(this);
    }

    private SharedPreferences getSharedPreferences() {
        if (mPreferences != null) return mPreferences;
        mPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        mPreferences.registerOnSharedPreferenceChangeListener(this);
        return mPreferences;
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
    }

    @Override
    public void onLowMemory() {
        final DependencyHolder holder = DependencyHolder.get(this);
        super.onLowMemory();
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences preferences, final String key) {
        switch (key) {
            case KEY_REFRESH_INTERVAL: {
                stopService(new Intent(this, RefreshService.class));
                Utils.startRefreshServiceIfNeeded(this);
                break;
            }
            case KEY_ENABLE_PROXY:
            case KEY_PROXY_HOST:
            case KEY_PROXY_PORT:
            case KEY_PROXY_TYPE:
            case KEY_PROXY_USERNAME:
            case KEY_PROXY_PASSWORD:
            case KEY_CONNECTION_TIMEOUT:
            case KEY_RETRY_ON_NETWORK_ISSUE: {
                HttpClientFactory.reloadConnectivitySettings(this);
                break;
            }
            case KEY_DNS_SERVER:
            case KEY_TCP_DNS_QUERY:
            case KEY_BUILTIN_DNS_RESOLVER: {
                reloadDnsSettings();
                break;
            }
            case KEY_CONSUMER_KEY:
            case KEY_CONSUMER_SECRET:
            case KEY_API_URL_FORMAT:
            case KEY_AUTH_TYPE:
            case KEY_SAME_OAUTH_SIGNING_URL: {
                final Editor editor = preferences.edit();
                editor.putLong(KEY_API_LAST_CHANGE, System.currentTimeMillis());
                editor.apply();
                break;
            }
            case KEY_EMOJI_SUPPORT: {
                DependencyHolder.get(this).getExternalThemeManager().reloadEmojiPreferences();
                break;
            }
            case KEY_THEME: {
                resetTheme(preferences);
                Config.markChanged(this, VALUE_THEME_NAME_LIGHT, VALUE_THEME_NAME_DARK);
                break;
            }
            case KEY_THEME_BACKGROUND: {
                Config.markChanged(this, VALUE_THEME_NAME_LIGHT, VALUE_THEME_NAME_DARK);
                break;
            }
            case KEY_THEME_COLOR: {
                final int themeColor = preferences.getInt(key, ContextCompat.getColor(this,
                        R.color.branding_color));
                ATE.config(this, VALUE_THEME_NAME_LIGHT)
                        .primaryColor(themeColor)
                        .accentColor(themeColor)
                        .coloredActionBar(true)
                        .commit();
                ATE.config(this, VALUE_THEME_NAME_DARK)
                        .accentColor(themeColor)
                        .coloredActionBar(false)
                        .commit();
                break;
            }
        }
    }

    private void resetTheme(SharedPreferences preferences) {
        switch (ThemeUtils.getLocalNightMode(preferences)) {
            case AppCompatDelegate.MODE_NIGHT_AUTO: {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
                break;
            }
            case AppCompatDelegate.MODE_NIGHT_YES: {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            }
            default: {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            }
        }
    }

    private void reloadDnsSettings() {
        DependencyHolder holder = DependencyHolder.get(this);
        final TwidereDns dns = holder.getDns();
        dns.reloadDnsSettings();
    }


    private void initializeAsyncTask() {
        // AsyncTask class needs to be loaded in UI thread.
        // So we load it here to comply the rule.
        try {
            Class.forName(AsyncTask.class.getName());
        } catch (final ClassNotFoundException ignore) {
        }
    }

    public ApplicationModule getApplicationModule() {
        if (mApplicationModule != null) return mApplicationModule;
        return mApplicationModule = new ApplicationModule(this);
    }

    @Nullable
    public static TwittnukerApplication getInstance() {
        return sInstance;
    }
}