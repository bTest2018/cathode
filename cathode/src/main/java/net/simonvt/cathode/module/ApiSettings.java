/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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

package net.simonvt.cathode.module;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.io.IOException;
import javax.inject.Inject;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.api.TraktSettings;
import net.simonvt.cathode.api.entity.AccessToken;
import net.simonvt.cathode.api.entity.TokenRequest;
import net.simonvt.cathode.api.enumeration.GrantType;
import net.simonvt.cathode.api.service.AuthorizationService;
import net.simonvt.cathode.settings.Settings;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public class ApiSettings implements TraktSettings {

  private static volatile ApiSettings instance;

  public static ApiSettings getInstance(Context context) {
    if (instance == null) {
      synchronized (ApiSettings.class) {
        if (instance == null) {
          instance = new ApiSettings(context);
        }
      }
    }

    return instance;
  }

  private Context context;

  private SharedPreferences settings;

  @Inject AuthorizationService authService;

  private boolean refreshingToken;

  public ApiSettings(Context context) {
    this.context = context;
    settings = PreferenceManager.getDefaultSharedPreferences(context);
  }

  @Override public String getAccessToken() {
    synchronized (ApiSettings.class) {
      return settings.getString(Settings.TRAKT_ACCESS_TOKEN, null);
    }
  }

  @Override public String getRefreshToken() {
    synchronized (ApiSettings.class) {
      return settings.getString(Settings.TRAKT_REFRESH_TOKEN, null);
    }
  }

  @Override public void updateTokens(AccessToken tokens) {
    synchronized (ApiSettings.class) {
      settings.edit()
          .putString(Settings.TRAKT_ACCESS_TOKEN, tokens.getAccessToken())
          .putString(Settings.TRAKT_REFRESH_TOKEN, tokens.getRefreshToken())
          .apply();
    }
  }

  @Override public String getClientId() {
    return BuildConfig.TRAKT_CLIENT_ID;
  }

  @Override public String getSecret() {
    return BuildConfig.TRAKT_SECRET;
  }

  @Override public String getRedirectUrl() {
    return BuildConfig.TRAKT_REDIRECT_URL;
  }

  private void clearRefreshToken() {
    synchronized (ApiSettings.class) {
      settings.edit().remove(Settings.TRAKT_REFRESH_TOKEN).apply();
    }
  }

  @Override public String refreshToken() {
    synchronized (ApiSettings.class) {
      if (authService == null) {
        CathodeApp.inject(context, this);
      }

      final String refreshToken = getRefreshToken();
      if (refreshToken == null || refreshingToken) {
        return null;
      }

      TokenRequest tokenRequest =
          TokenRequest.refreshToken(refreshToken, getClientId(), getSecret(), getRedirectUrl(),
              GrantType.REFRESH_TOKEN);

      try {
        refreshingToken = true;

        Timber.d("Getting new tokens, with refresh token: %s", refreshToken);
        Call<AccessToken> call = authService.getToken(tokenRequest);
        Response<AccessToken> response = call.execute();
        if (response.isSuccessful()) {
          AccessToken token = response.body();
          updateTokens(token);
          return token.getAccessToken();
        } else {
          if (response.code() == 401) {
            clearRefreshToken();
            return null;
          }

          String message = "Code: " + response.code();
          Timber.e(new TokenRefreshFailedException(message), "Unable to get token");
        }
      } catch (IOException e) {
        Timber.e(e, "Unable to get new tokens");
      } finally {
        refreshingToken = false;
      }

      return null;
    }
  }

  public static class TokenRefreshFailedException extends Exception {

    public TokenRefreshFailedException() {
      super();
    }

    public TokenRefreshFailedException(String detailMessage) {
      super(detailMessage);
    }

    public TokenRefreshFailedException(String detailMessage, Throwable throwable) {
      super(detailMessage, throwable);
    }

    public TokenRefreshFailedException(Throwable throwable) {
      super(throwable);
    }
  }
}
