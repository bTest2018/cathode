package net.simonvt.trakt.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.InjectView;
import butterknife.Views;
import javax.inject.Inject;
import net.simonvt.trakt.R;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.provider.TraktDatabase;
import net.simonvt.trakt.scheduler.EpisodeTaskScheduler;
import net.simonvt.trakt.scheduler.ShowTaskScheduler;
import net.simonvt.trakt.ui.LibraryType;
import net.simonvt.trakt.util.DateUtils;
import net.simonvt.trakt.util.LogWrapper;
import net.simonvt.trakt.widget.OverflowView;
import net.simonvt.trakt.widget.RemoteImageView;

public class ShowsAdapter extends CursorAdapter {

  private static final String TAG = "ShowsAdapter";

  public static final String[] PROJECTION = new String[] {
      TraktDatabase.Tables.SHOWS + "." + BaseColumns._ID,
      TraktDatabase.Tables.SHOWS + "." + TraktContract.Shows.TITLE,
      TraktDatabase.Tables.SHOWS + "." + TraktContract.Shows.POSTER,
      TraktDatabase.Tables.SHOWS + "." + TraktContract.Shows.AIRDATE_COUNT,
      TraktContract.Shows.UNAIRED_COUNT,
      TraktDatabase.Tables.SHOWS + "." + TraktContract.Shows.WATCHED_COUNT,
      TraktDatabase.Tables.SHOWS + "." + TraktContract.Shows.STATUS,
      TraktDatabase.Tables.EPISODES + "." + TraktContract.Episodes.TITLE,
      TraktDatabase.Tables.EPISODES + "." + TraktContract.Episodes.FIRST_AIRED,
      TraktDatabase.Tables.EPISODES + "." + TraktContract.Episodes.SEASON,
      TraktDatabase.Tables.EPISODES + "." + TraktContract.Episodes.EPISODE,
  };

  @Inject EpisodeTaskScheduler scheduler;

  @Inject ShowTaskScheduler showScheduler;

  private final LibraryType libraryType;

  public ShowsAdapter(Context context, LibraryType libraryType) {
    this(context, null, libraryType);
  }

  public ShowsAdapter(Context context, Cursor cursor, LibraryType libraryType) {
    super(context, cursor, 0);
    TraktApp.inject(context, this);
    this.libraryType = libraryType;
  }

  @Override
  public View newView(Context context, Cursor cursor, ViewGroup parent) {
    View v = LayoutInflater.from(context).inflate(R.layout.list_row_show, parent, false);

    ViewHolder vh = new ViewHolder(v);
    v.setTag(vh);

    return v;
  }

  @Override
  public void bindView(View view, Context context, Cursor cursor) {
    final long id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));

    final String showPosterUrl =
        cursor.getString(cursor.getColumnIndex(TraktContract.Shows.POSTER));
    final String showTitle = cursor.getString(cursor.getColumnIndex(TraktContract.Shows.TITLE));
    final String showStatus = cursor.getString(cursor.getColumnIndex(TraktContract.Shows.STATUS));

    final int showAirdateCount =
        cursor.getInt(cursor.getColumnIndex(TraktContract.Shows.AIRDATE_COUNT));
    final int showUnairedCount =
        cursor.getInt(cursor.getColumnIndex(TraktContract.Shows.UNAIRED_COUNT));
    int showTypeCount = 0;
    switch (libraryType) {
      case WATCHED:
      case WATCHLIST:
        showTypeCount = cursor.getInt(cursor.getColumnIndex(TraktContract.Shows.WATCHED_COUNT));
        break;

      case COLLECTION:
        showTypeCount =
            cursor.getInt(cursor.getColumnIndex(TraktContract.Shows.IN_COLLECTION_COUNT));
        break;
    }

    final int showAiredCount = showAirdateCount - showUnairedCount;

    final String episodeTitle =
        cursor.getString(cursor.getColumnIndex(TraktContract.Episodes.TITLE));
    final long episodeFirstAired =
        cursor.getLong(cursor.getColumnIndex(TraktContract.Episodes.FIRST_AIRED));
    final int episodeSeasonNumber =
        cursor.getInt(cursor.getColumnIndex(TraktContract.Episodes.SEASON));
    final int episodeNumber = cursor.getInt(cursor.getColumnIndex(TraktContract.Episodes.EPISODE));

    ViewHolder vh = (ViewHolder) view.getTag();

    vh.title.setText(showTitle);

    vh.progressBar.setMax(showAiredCount);
    vh.progressBar.setProgress(showTypeCount);

    vh.watched.setText(showTypeCount + "/" + showAiredCount);

    String episodeText;
    if (episodeTitle == null) {
      episodeText = showStatus;
      vh.firstAired.setVisibility(View.GONE);
    } else {
      episodeText = "Next: " + episodeSeasonNumber + "x" + episodeNumber + " " + episodeTitle;
      vh.firstAired.setVisibility(View.VISIBLE);
      vh.firstAired.setText(DateUtils.millisToString(mContext, episodeFirstAired, false));
    }
    vh.nextEpisode.setText(episodeText);
    vh.nextEpisode.setEnabled(episodeTitle != null);

    vh.overflow.setVisibility(showAiredCount > 0 ? View.VISIBLE : View.INVISIBLE);
    vh.overflow.setListener(new OverflowView.OverflowActionListener() {
      @Override
      public void onPopupShown() {
      }

      @Override
      public void onPopupDismissed() {
      }

      @Override
      public void onActionSelected(int action) {
        switch (action) {
          case R.id.action_watchlist_remove:
            showScheduler.setIsInWatchlist(id, false);
            break;

          case R.id.action_watched:
            showScheduler.watchedNext(id);
            LogWrapper.v(TAG, "Watched item: " + id);
            break;

          case R.id.action_watched_all:
            showScheduler.setWatched(id, true);
            break;

          case R.id.action_unwatch_all:
            showScheduler.setWatched(id, false);
            break;

          case R.id.action_collection_add:
            showScheduler.collectedNext(id);
            LogWrapper.v(TAG, "Watched item: " + id);
            break;

          case R.id.action_collection_add_all:
            showScheduler.setIsInCollection(id, true);
            break;

          case R.id.action_collection_remove_all:
            showScheduler.setIsInCollection(id, false);
            break;
        }
      }
    });

    vh.overflow.removeItems();
    switch (libraryType) {
      case WATCHLIST:
        if (showAiredCount - showTypeCount > 0) {
          vh.overflow.addItem(R.id.action_watchlist_remove, R.string.action_watchlist_remove);
        }

      case WATCHED:
        if (showAiredCount - showTypeCount > 0) {
          if (episodeTitle != null) {
            vh.overflow.addItem(R.id.action_watched, R.string.action_watched_next);
          }
          if (showTypeCount < showAiredCount) {
            vh.overflow.addItem(R.id.action_watched_all, R.string.action_watched_all);
          }
        }
        if (showTypeCount > 0) {
          vh.overflow.addItem(R.id.action_unwatch_all, R.string.action_unwatch_all);
        }
        break;

      case COLLECTION:
        if (showAiredCount - showTypeCount > 0) {
          vh.overflow.addItem(R.id.action_collection_add, R.string.action_collect_next);
          if (showTypeCount < showAiredCount) {
            vh.overflow.addItem(R.id.action_collection_add_all, R.string.action_collection_add_all);
          }
        }
        if (showTypeCount > 0) {
          vh.overflow
              .addItem(R.id.action_collection_remove_all, R.string.action_collection_remove_all);
        }
        break;
    }

    vh.poster.setImage(showPosterUrl);
  }

  public static class ViewHolder {

    @InjectView(R.id.infoParent) View infoParent;
    @InjectView(R.id.title) TextView title;
    @InjectView(R.id.watched) TextView watched;
    @InjectView(R.id.progress) ProgressBar progressBar;
    @InjectView(R.id.nextEpisode) TextView nextEpisode;
    @InjectView(R.id.firstAired) TextView firstAired;
    @InjectView(R.id.overflow) OverflowView overflow;
    @InjectView(R.id.poster) RemoteImageView poster;

    ViewHolder(View v) {
      Views.inject(this, v);
    }
  }
}
