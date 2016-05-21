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

package net.simonvt.cathode.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.ui.fragment.DashboardFragment;
import net.simonvt.cathode.widget.RemoteImageView;
import net.simonvt.schematic.Cursors;

public class DashboardUpcomingShowsAdapter
    extends RecyclerCursorAdapter<DashboardUpcomingShowsAdapter.ViewHolder> {

  private static final String COLUMN_EPISODE_ID = "episodeId";
  private static final String COLUMN_EPISODE_LAST_UPDATED = "episodeLastUpdated";

  public static final String[] PROJECTION = new String[] {
      Tables.SHOWS + "." + ShowColumns.ID,
      Tables.SHOWS + "." + ShowColumns.TITLE,
      Tables.SHOWS + "." + ShowColumns.OVERVIEW,
      Tables.SHOWS + "." + ShowColumns.POSTER,
      Tables.SHOWS + "." + ShowColumns.STATUS,
      ShowColumns.WATCHING,
      Tables.SHOWS + "." + ShowColumns.LAST_MODIFIED,
      Tables.EPISODES + "." + EpisodeColumns.ID + " AS " + COLUMN_EPISODE_ID,
      Tables.EPISODES + "." + EpisodeColumns.TITLE,
      Tables.EPISODES + "." + EpisodeColumns.FIRST_AIRED,
      Tables.EPISODES + "." + EpisodeColumns.SEASON,
      Tables.EPISODES + "." + EpisodeColumns.EPISODE,
      Tables.EPISODES + "." + EpisodeColumns.LAST_MODIFIED + " AS " + COLUMN_EPISODE_LAST_UPDATED,
  };

  private DashboardFragment.OverviewCallback callback;

  public DashboardUpcomingShowsAdapter(Context context,
      DashboardFragment.OverviewCallback callback) {
    super(context);
    this.callback = callback;
  }

  @Override public long getLastModified(int position) {
    Cursor cursor = getCursor(position);

    final long showLastModified = cursor.getLong(
        cursor.getColumnIndexOrThrow(DatabaseContract.LastModifiedColumns.LAST_MODIFIED));
    final long episodeLastModified =
        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_EPISODE_LAST_UPDATED));

    return showLastModified + episodeLastModified;
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.list_row_dashboard_show_upcoming, parent, false);
    final ViewHolder holder = new ViewHolder(view);

    view.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        final int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
          Cursor cursor = getCursor(position);
          final String title = Cursors.getString(cursor, ShowColumns.TITLE);
          final String overview = Cursors.getString(cursor, ShowColumns.OVERVIEW);
          callback.onDisplayShow(holder.getItemId(), title, overview);
        }
      }
    });

    return holder;
  }

  @Override protected void onBindViewHolder(ViewHolder holder, Cursor cursor, int position) {
    final String poster = Cursors.getString(cursor, ShowColumns.POSTER);
    final String title = Cursors.getString(cursor, ShowColumns.TITLE);
    final boolean watching = cursor.getInt(cursor.getColumnIndex(ShowColumns.WATCHING)) == 1;
    final String episodeTitle = Cursors.getString(cursor, EpisodeColumns.TITLE);
    final int season = Cursors.getInt(cursor, EpisodeColumns.SEASON);
    final int episode = Cursors.getInt(cursor, EpisodeColumns.EPISODE);

    holder.poster.setImage(poster);
    holder.title.setText(title);

    String episodeText;
    if (watching) {
      episodeText = getContext().getString(R.string.show_watching);
    } else {
      episodeText =
          getContext().getString(R.string.upcoming_episode_next, season, episode, episodeTitle);
    }

    holder.nextEpisode.setText(episodeText);
  }

  static class ViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.poster) RemoteImageView poster;
    @BindView(R.id.title) TextView title;
    @BindView(R.id.nextEpisode) TextView nextEpisode;

    public ViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }
}
