/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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

package net.simonvt.cathode.remote.action;

import javax.inject.Inject;
import net.simonvt.cathode.api.body.RemoveHistoryBody;
import net.simonvt.cathode.api.entity.SyncResponse;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.remote.CallJob;
import retrofit2.Call;

public class RemoveHistoryItem extends CallJob<SyncResponse> {

  @Inject transient SyncService syncService;

  private long historyId;

  public RemoveHistoryItem(long historyId) {
    this.historyId = historyId;
  }

  @Override public String key() {
    return "RemoveHistoryItem&historyId=" + historyId;
  }

  @Override public int getPriority() {
    return PRIORITY_ACTIONS;
  }

  @Override public Call<SyncResponse> getCall() {
    RemoveHistoryBody body = new RemoveHistoryBody();
    body.id(historyId);
    return syncService.removeHistory(body);
  }

  @Override public boolean handleResponse(SyncResponse response) {
    return true;
  }
}
