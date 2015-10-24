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
package net.simonvt.cathode.remote.action.movies;

import javax.inject.Inject;
import net.simonvt.cathode.api.body.SyncItems;
import net.simonvt.cathode.api.entity.SyncResponse;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.api.util.TimeUtils;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import retrofit.Call;

public class WatchedMovie extends CallJob<SyncResponse> {

  @Inject transient SyncService syncService;

  private long traktId;

  private boolean watched;

  private String watchedAt;

  public WatchedMovie(long traktId, boolean watched, String watchedAt) {
    super(Flags.REQUIRES_AUTH);
    this.traktId = traktId;
    this.watched = watched;
    this.watchedAt = watchedAt;
  }

  @Override public String key() {
    return "WatchedMovie"
        + "&traktId="
        + traktId
        + "&watched="
        + watchedAt
        + "&watchedAt"
        + watchedAt;
  }

  @Override public int getPriority() {
    return PRIORITY_ACTIONS;
  }

  @Override public boolean allowDuplicates() {
    return true;
  }

  @Override public Call<SyncResponse> getCall() {
    SyncItems items = new SyncItems();
    SyncItems.Movie movie = items.movie(traktId);
    if (watched) {
      movie.watchedAt(watchedAt);
      return syncService.watched(items);
    } else {
      return syncService.unwatched(items);
    }
  }

  @Override public void handleResponse(SyncResponse response) {
    MovieWrapper.setWatched(getContentResolver(), traktId, watched, TimeUtils.getMillis(watchedAt));
  }
}
