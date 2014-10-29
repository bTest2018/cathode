/*
 * Copyright (C) 2013 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.simonvt.cathode;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import com.crashlytics.android.Crashlytics;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import dagger.ObjectGraph;
import javax.inject.Inject;
import net.simonvt.cathode.api.TraktModule;
import net.simonvt.cathode.event.AuthFailedEvent;
import net.simonvt.cathode.module.AppModule;
import net.simonvt.cathode.service.AccountAuthenticator;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.HomeActivity;
import net.simonvt.cathode.util.DateUtils;
import timber.log.Timber;

public class CathodeApp extends Application {

  private static final String TAG = "CathodeApp";

  public static final boolean DEBUG = BuildConfig.DEBUG;

  private static final int AUTH_NOTIFICATION = 2;

  private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

  private ObjectGraph objectGraph;

  @Inject Bus bus;

  @Override public void onCreate() {
    super.onCreate();
    if (BuildConfig.DEBUG) {
      Timber.plant(new Timber.DebugTree());

      StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
      StrictMode.setThreadPolicy(
          new StrictMode.ThreadPolicy.Builder().detectAll().permitDiskReads().penaltyLog().build());
    } else {
      Crashlytics.start(this);
      Timber.plant(new CrashlyticsTree());
    }

    upgrade();

    if (!accountExists(this)) {
      setupAccount(this);
    }

    objectGraph = ObjectGraph.create(new AppModule(this));
    objectGraph.plus(new TraktModule());

    objectGraph.inject(this);

    bus.register(this);
  }

  private void upgrade() {
    // TODO: Delete CathodeDatabase
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    final int currentVersion = settings.getInt(Settings.VERSION_CODE, -1);

    if (currentVersion == -1) {
      settings.edit().putInt(Settings.VERSION_CODE, BuildConfig.VERSION_CODE).apply();
      return;
    }

    if (currentVersion != BuildConfig.VERSION_CODE) {
      if (currentVersion < 20000) {
        removeAccount(this);
      }

      settings.edit().putInt(Settings.VERSION_CODE, BuildConfig.VERSION_CODE).apply();
    }
  }

  @Subscribe public void onAuthFailure(AuthFailedEvent event) {
    Timber.tag(TAG).i("onAuthFailure");
    if (!accountExists(this)) return; // User has logged out, ignore.

    Account account = getAccount(this);

    Intent intent = new Intent(this, HomeActivity.class);
    intent.setAction(HomeActivity.ACTION_LOGIN);
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

    PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);

    Notification.Builder builder = new Notification.Builder(this) //
        .setSmallIcon(R.drawable.ic_noti_error)
        .setTicker(getString(R.string.auth_failed))
        .setContentTitle(getString(R.string.auth_failed))
        .setContentText(getString(R.string.auth_failed_desc, account.name))
        .setContentIntent(pi)
        .setPriority(Notification.PRIORITY_HIGH);

    NotificationManager nm = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
    nm.notify(AUTH_NOTIFICATION, builder.build());
  }

  public static void inject(Context context) {
    ((CathodeApp) context.getApplicationContext()).objectGraph.inject(context);
  }

  public static void inject(Context context, Object object) {
    ((CathodeApp) context.getApplicationContext()).objectGraph.inject(object);
  }

  public static boolean accountExists(Context context) {
    AccountManager am = AccountManager.get(context);
    Account[] accounts = am.getAccountsByType(context.getString(R.string.accountType));

    return accounts.length > 0;
  }

  public static Account getAccount(Context context) {
    AccountManager accountManager = AccountManager.get(context);
    Account[] accounts = accountManager.getAccountsByType(context.getString(R.string.accountType));
    return accounts.length > 0 ? accounts[0] : null;
  }

  public static void setupAccount(Context context) {
    removeAccount(context);

    AccountAuthenticator.allowRemove = false;
    AccountManager manager = AccountManager.get(context);

    // TODO: Can I get the username?
    Account account = new Account("Cathode", context.getString(R.string.accountType));

    manager.addAccountExplicitly(account, null, null);

    ContentResolver.setIsSyncable(account, BuildConfig.PROVIDER_AUTHORITY, 1);
    ContentResolver.setSyncAutomatically(account, BuildConfig.PROVIDER_AUTHORITY, true);
    ContentResolver.addPeriodicSync(account, BuildConfig.PROVIDER_AUTHORITY, new Bundle(),
        12 * DateUtils.HOUR_IN_SECONDS);

    ContentResolver.setIsSyncable(account, CalendarContract.AUTHORITY, 1);
    ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, true);
    ContentResolver.addPeriodicSync(account, CalendarContract.AUTHORITY, new Bundle(),
        12 * DateUtils.HOUR_IN_SECONDS);
  }

  public static void removeAccount(Context context) {
    AccountAuthenticator.allowRemove = true;
    AccountManager am = AccountManager.get(context);
    Account[] accounts = am.getAccountsByType(context.getString(R.string.accountType));
    for (Account account : accounts) {
      ContentResolver.removePeriodicSync(account, BuildConfig.PROVIDER_AUTHORITY, new Bundle());
      ContentResolver.removePeriodicSync(account, CalendarContract.AUTHORITY, new Bundle());
      am.removeAccount(account, null, null);
    }
  }
}
