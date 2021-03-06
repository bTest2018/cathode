/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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
package net.simonvt.cathode.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import net.simonvt.cathode.common.util.WakeLock;
import net.simonvt.cathode.settings.Settings;

public class NotificationReceiver extends BroadcastReceiver {

  @Override public void onReceive(Context context, Intent intent) {
    if (Settings.get(context).getBoolean(Settings.NOTIFICACTIONS_ENABLED, false)) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        PendingResult pendingResult = goAsync();
        NotificationHelper.displayNotifications(context);
        NotificationHelper.scheduleNextNotification(context);
        pendingResult.finish();
      } else {
        WakeLock.acquire(context, NotificationService.LOCK_TAG);
        NotificationService.start(context);
      }
    }
  }
}
