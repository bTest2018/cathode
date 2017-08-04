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

package net.simonvt.cathode.remote.action.comments;

import java.io.IOException;
import javax.inject.Inject;
import net.simonvt.cathode.api.body.CommentBody;
import net.simonvt.cathode.api.entity.Comment;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.api.service.CommentsService;
import net.simonvt.cathode.common.event.RequestFailedEvent;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.jobs.R;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.cathode.remote.sync.SyncUserActivity;
import retrofit2.Call;
import retrofit2.Response;

public class AddCommentJob extends CallJob<Comment> {

  @Inject transient CommentsService commentsService;

  private ItemType type;
  private long traktId;
  private String comment;
  private Boolean spoiler;

  public AddCommentJob(ItemType type, long traktId, String comment, boolean spoiler) {
    super(Flags.REQUIRES_AUTH);
    this.type = type;
    this.traktId = traktId;
    this.comment = comment;
    this.spoiler = spoiler;
  }

  @Override public String key() {
    return "AddCommentJob?type=" + type.toString() + "&traktId=" + traktId + "&comment=" + comment;
  }

  @Override public boolean allowDuplicates() {
    return true;
  }

  @Override public int getPriority() {
    return JobPriority.ACTIONS;
  }

  @Override public Call<Comment> getCall() {
    CommentBody body = CommentBody.comment(comment);
    if (spoiler) {
      body.spoiler();
    }

    switch (type) {
      case SHOW:
        body.show(traktId);
        break;

      case EPISODE:
        body.episode(traktId);
        break;

      case MOVIE:
        body.movie(traktId);
        break;

      default:
        throw new IllegalStateException("Unknown type: " + type);
    }

    return commentsService.post(body);
  }

  @Override protected boolean isError(Response<Comment> response) throws IOException {
    final int statusCode = response.code();
    if (statusCode == 422) {
      RequestFailedEvent.post(R.string.comment_submit_error);
      return false;
    }

    return super.isError(response);
  }

  @Override public boolean handleResponse(Comment comment) {
    queue(new SyncUserActivity());
    return true;
  }
}
