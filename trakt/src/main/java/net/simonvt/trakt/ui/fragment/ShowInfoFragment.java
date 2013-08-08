package net.simonvt.trakt.ui.fragment;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import butterknife.InjectView;
import butterknife.Views;
import com.squareup.otto.Bus;
import javax.inject.Inject;
import net.simonvt.trakt.R;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.event.OnTitleChangedEvent;
import net.simonvt.trakt.provider.CollectLoader;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.provider.TraktContract.Shows;
import net.simonvt.trakt.provider.WatchedLoader;
import net.simonvt.trakt.remote.TraktTaskQueue;
import net.simonvt.trakt.scheduler.EpisodeTaskScheduler;
import net.simonvt.trakt.scheduler.ShowTaskScheduler;
import net.simonvt.trakt.ui.LibraryType;
import net.simonvt.trakt.ui.ShowsNavigationListener;
import net.simonvt.trakt.ui.adapter.SeasonsAdapter;
import net.simonvt.trakt.ui.dialog.RatingDialog;
import net.simonvt.trakt.util.DateUtils;
import net.simonvt.trakt.widget.HiddenPaneLayout;
import net.simonvt.trakt.widget.OverflowView;
import net.simonvt.trakt.widget.RemoteImageView;

public class ShowInfoFragment extends ProgressFragment {

  private static final String TAG = "ShowInfoFragment";

  private static final String ARG_SHOWID = "net.simonvt.trakt.ui.fragment.ShowInfoFragment.showId";
  private static final String ARG_TITLE = "net.simonvt.trakt.ui.fragment.ShowInfoFragment.title";
  private static final String ARG_TYPE = "net.simonvt.trakt.ui.fragment.ShowInfoFragment.type";

  private static final String DIALOG_RATING =
      "net.simonvt.trakt.ui.fragment.ShowInfoFragment.ratingDialog";

  private static final int LOADER_SHOW = 10;
  private static final int LOADER_GENRES = 11;
  private static final int LOADER_WATCH = 12;
  private static final int LOADER_COLLECT = 13;
  private static final int LOADER_SEASONS = 14;

  private static final String[] SHOW_PROJECTION = new String[] {
      Shows.TITLE, Shows.YEAR, Shows.AIR_TIME, Shows.AIR_DAY, Shows.NETWORK, Shows.CERTIFICATION,
      Shows.BANNER, Shows.RATING_PERCENTAGE, Shows.RATING, Shows.OVERVIEW, Shows.IN_WATCHLIST,
      Shows.IN_COLLECTION_COUNT, Shows.WATCHED_COUNT,
  };

  private static final String[] EPISODE_PROJECTION = new String[] {
      BaseColumns._ID, TraktContract.Episodes.TITLE, TraktContract.Episodes.SCREEN,
      TraktContract.Episodes.FIRST_AIRED, TraktContract.Episodes.SEASON,
      TraktContract.Episodes.EPISODE,
  };

  private static final String[] GENRES_PROJECTION = new String[] {
      TraktContract.ShowGenres.GENRE,
  };

  private ShowsNavigationListener navigationCallbacks;

  private long showId;

  @InjectView(R.id.hiddenPaneLayout) HiddenPaneLayout hiddenPaneLayout;

  @InjectView(R.id.seasons) ListView seasons;
  private SeasonsAdapter seasonsAdapter;

  @InjectView(R.id.title) TextView title;
  @InjectView(R.id.ratingContainer) View ratingContainer;
  @InjectView(R.id.rating) RatingBar rating;
  @InjectView(R.id.allRatings) TextView allRatings;
  //@InjectView(R.id.year) TextView year;
  @InjectView(R.id.airtime) TextView airTime;
  @InjectView(R.id.certification) TextView certification;
  @InjectView(R.id.banner) RemoteImageView banner;
  @InjectView(R.id.genres) TextView genres;
  @InjectView(R.id.overview) TextView overview;
  @InjectView(R.id.isWatched) TextView watched;
  @InjectView(R.id.inCollection) TextView collection;
  @InjectView(R.id.inWatchlist) TextView watchlist;

  @InjectView(R.id.episodesTitle) View episodesTitle;
  @InjectView(R.id.episodes) LinearLayout episodes;

  @InjectView(R.id.watchTitle) View watchTitle;
  @InjectView(R.id.collectTitle) View collectTitle;

  @InjectView(R.id.toWatch) View toWatch;
  private EpisodeHolder toWatchHolder;
  private long toWatchId = -1;

  @InjectView(R.id.lastWatched) View lastWatched;
  private EpisodeHolder lastWatchedHolder;
  private long lastWatchedId = -1;

  @InjectView(R.id.toCollect) View toCollect;
  private EpisodeHolder toCollectHolder;
  private long toCollectId = -1;

  @InjectView(R.id.lastCollected) View lastCollected;
  private EpisodeHolder lastCollectedHolder;
  private long lastCollectedId = -1;

  static class EpisodeHolder {

    @InjectView(R.id.episode) TextView episode;
    @InjectView(R.id.episodeBanner) RemoteImageView episodeBanner;
    @InjectView(R.id.episodeTitle) TextView episodeTitle;
    @InjectView(R.id.episodeAirTime) TextView episodeAirTime;
    @InjectView(R.id.episodeEpisode) TextView episodeEpisode;
    @InjectView(R.id.episodeOverflow) OverflowView episodeOverflow;

    public EpisodeHolder(View v) {
      Views.inject(this, v);
    }
  }

  @Inject ShowTaskScheduler showScheduler;
  @Inject EpisodeTaskScheduler episodeScheduler;
  @Inject TraktTaskQueue queue;

  @Inject Bus bus;

  private String showTitle;

  private int currentRating;

  private LibraryType type;

  public static Bundle getArgs(long showId, String title, LibraryType type) {
    Bundle args = new Bundle();
    args.putLong(ARG_SHOWID, showId);
    args.putString(ARG_TITLE, title);
    args.putSerializable(ARG_TYPE, type);
    return args;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      navigationCallbacks = (ShowsNavigationListener) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString() + " must implement ShowsNavigationListener");
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    TraktApp.inject(getActivity(), this);

    setHasOptionsMenu(true);

    Bundle args = getArguments();
    showId = args.getLong(ARG_SHOWID);
    showTitle = args.getString(ARG_TITLE);
    type = (LibraryType) args.getSerializable(ARG_TYPE);

    seasonsAdapter = new SeasonsAdapter(getActivity(), type);
  }

  @Override
  public String getTitle() {
    return showTitle == null ? "" : showTitle;
  }

  @Override
  public boolean onBackPressed() {
    final int state = hiddenPaneLayout.getState();
    if (state == HiddenPaneLayout.STATE_OPEN || state == HiddenPaneLayout.STATE_OPENING) {
      hiddenPaneLayout.close();
      return true;
    }

    return super.onBackPressed();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_show_info, container, false);
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    seasons.setAdapter(seasonsAdapter);
    seasons.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Cursor c = (Cursor) seasonsAdapter.getItem(position);
        navigationCallbacks.onDisplaySeason(showId, id, showTitle,
            c.getInt(c.getColumnIndex(TraktContract.Seasons.SEASON)), type);
      }
    });

    ratingContainer.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        RatingDialog.newInstance(RatingDialog.Type.SHOW, showId, currentRating)
            .show(getFragmentManager(), DIALOG_RATING);
      }
    });

    if (toWatch != null) {
      toWatchHolder = new EpisodeHolder(toWatch);
      toWatch.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          if (toWatchId != -1) navigationCallbacks.onDisplayEpisode(toWatchId, showTitle);
        }
      });

      toWatchHolder.episodeOverflow.addItem(R.id.action_watched, R.string.action_watched);
      toWatchHolder.episodeOverflow.setListener(new OverflowView.OverflowActionListener() {
        @Override
        public void onPopupShown() {
        }

        @Override
        public void onPopupDismissed() {
        }

        @Override
        public void onActionSelected(int action) {
          switch (action) {
            case R.id.action_watched:
              if (toWatchId != -1) {
                episodeScheduler.setWatched(toWatchId, true);
              }
              break;
          }
        }
      });
    }

    if (lastWatched != null) {
      lastWatchedHolder = new EpisodeHolder(lastWatched);
      lastWatched.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          if (lastWatchedId != -1) {
            navigationCallbacks.onDisplayEpisode(lastWatchedId, showTitle);
          }
        }
      });

      lastWatchedHolder.episodeOverflow.addItem(R.id.action_unwatched, R.string.action_unwatched);
      lastWatchedHolder.episodeOverflow.setListener(new OverflowView.OverflowActionListener() {
        @Override
        public void onPopupShown() {
        }

        @Override
        public void onPopupDismissed() {
        }

        @Override
        public void onActionSelected(int action) {
          switch (action) {
            case R.id.action_watched:
              if (lastWatchedId != -1) {
                episodeScheduler.setWatched(lastWatchedId, true);
              }
              break;
          }
        }
      });
    }

    if (toCollect != null) {
      toCollectHolder = new EpisodeHolder(toCollect);
      toCollect.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          if (toCollectId != -1) navigationCallbacks.onDisplayEpisode(toCollectId, showTitle);
        }
      });

      toCollectHolder.episodeOverflow
          .addItem(R.id.action_collection_add, R.string.action_collection_add);
      toCollectHolder.episodeOverflow.setListener(new OverflowView.OverflowActionListener() {
        @Override
        public void onPopupShown() {
        }

        @Override
        public void onPopupDismissed() {
        }

        @Override
        public void onActionSelected(int action) {
          switch (action) {
            case R.id.action_collection_add:
              if (toCollectId != -1) {
                episodeScheduler.setIsInCollection(toCollectId, true);
              }
              break;
          }
        }
      });
    }

    if (lastCollected != null) {
      lastCollectedHolder = new EpisodeHolder(lastCollected);
      lastCollected.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          if (lastCollectedId != -1) {
            navigationCallbacks.onDisplayEpisode(lastCollectedId, showTitle);
          }
        }
      });

      lastCollectedHolder.episodeOverflow
          .addItem(R.id.action_collection_remove, R.string.action_collection_remove);
      lastCollectedHolder.episodeOverflow.setListener(new OverflowView.OverflowActionListener() {
        @Override
        public void onPopupShown() {
        }

        @Override
        public void onPopupDismissed() {
        }

        @Override
        public void onActionSelected(int action) {
          switch (action) {
            case R.id.action_collection_add:
              if (lastCollectedId != -1) {
                episodeScheduler.setIsInCollection(lastCollectedId, true);
              }
              break;
          }
        }
      });
    }

    getLoaderManager().initLoader(LOADER_SHOW, null, loaderCallbacks);
    getLoaderManager().initLoader(LOADER_GENRES, null, genreCallbacks);
    getLoaderManager().initLoader(LOADER_WATCH, null, episodeWatchCallbacks);
    getLoaderManager().initLoader(LOADER_COLLECT, null, episodeCollectCallbacks);
    getLoaderManager().initLoader(LOADER_SEASONS, null, seasonsLoader);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.fragment_show_info, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_seasons:
        hiddenPaneLayout.toggle();
        return true;
    }

    return false;
  }

  @Override
  public void onDestroy() {
    if (getActivity().isFinishing() || isRemoving()) {
      getLoaderManager().destroyLoader(LOADER_SHOW);
      getLoaderManager().destroyLoader(LOADER_GENRES);
      getLoaderManager().destroyLoader(LOADER_WATCH);
      getLoaderManager().destroyLoader(LOADER_COLLECT);
      getLoaderManager().destroyLoader(LOADER_SEASONS);
    }
    super.onDestroy();
  }

  private void updateShowView(final Cursor cursor) {
    if (cursor == null || !cursor.moveToFirst()) return;

    String title = cursor.getString(cursor.getColumnIndex(Shows.TITLE));
    if (!title.equals(showTitle)) {
      showTitle = title;
      bus.post(new OnTitleChangedEvent());
    }
    final int year = cursor.getInt(cursor.getColumnIndex(Shows.YEAR));
    final String airTime = cursor.getString(cursor.getColumnIndex(Shows.AIR_TIME));
    final String airDay = cursor.getString(cursor.getColumnIndex(Shows.AIR_DAY));
    final String network = cursor.getString(cursor.getColumnIndex(Shows.NETWORK));
    final String certification = cursor.getString(cursor.getColumnIndex(Shows.CERTIFICATION));
    final String bannerUrl = cursor.getString(cursor.getColumnIndex(Shows.BANNER));
    if (bannerUrl != null) {
      banner.setImage(bannerUrl);
    }
    currentRating = cursor.getInt(cursor.getColumnIndex(Shows.RATING));
    final int ratingAll = cursor.getInt(cursor.getColumnIndex(Shows.RATING_PERCENTAGE));
    final String overview = cursor.getString(cursor.getColumnIndex(Shows.OVERVIEW));
    final boolean inWatchlist = cursor.getInt(cursor.getColumnIndex(Shows.IN_WATCHLIST)) == 1;
    final int inCollectionCount = cursor.getInt(cursor.getColumnIndex(Shows.IN_COLLECTION_COUNT));
    final int watchedCount = cursor.getInt(cursor.getColumnIndex(Shows.WATCHED_COUNT));

    rating.setProgress(currentRating);
    allRatings.setText(ratingAll + "%");

    watched.setVisibility(watchedCount > 0 ? View.VISIBLE : View.GONE);
    collection.setVisibility(inCollectionCount > 0 ? View.VISIBLE : View.GONE);
    watchlist.setVisibility(inWatchlist ? View.VISIBLE : View.GONE);

    // title.setText(title);
    // year.setText(String.valueOf(year));
    this.airTime.setText(airDay + " " + airTime + ", " + network);
    this.certification.setText(certification);
    // rating.setText(String.valueOf(rating));
    this.overview.setText(overview);

    setContentVisible(true);
  }

  private void updateGenreViews(final Cursor cursor) {
    StringBuilder sb = new StringBuilder();
    final int genreColumnIndex = cursor.getColumnIndex(TraktContract.ShowGenres.GENRE);

    cursor.moveToPosition(-1);

    while (cursor.moveToNext()) {
      sb.append(cursor.getString(genreColumnIndex));
      if (!cursor.isLast()) sb.append(", ");
    }

    genres.setText(sb.toString());
  }

  private void updateEpisodeWatchViews(Cursor cursor) {
    if (cursor.moveToFirst()) {
      toWatch.setVisibility(View.VISIBLE);
      watchTitle.setVisibility(View.VISIBLE);

      toWatchId = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));

      toWatchHolder.episodeTitle
          .setText(cursor.getString(cursor.getColumnIndex(TraktContract.Episodes.TITLE)));

      final long airTime =
          cursor.getLong(cursor.getColumnIndex(TraktContract.Episodes.FIRST_AIRED));
      final String airTimeStr = DateUtils.millisToString(getActivity(), airTime, false);
      toWatchHolder.episodeAirTime.setText(airTimeStr);

      final int season = cursor.getInt(cursor.getColumnIndex(TraktContract.Episodes.SEASON));
      final int episode = cursor.getInt(cursor.getColumnIndex(TraktContract.Episodes.EPISODE));
      toWatchHolder.episodeEpisode.setText("S" + season + "E" + episode);

      final String bannerUrl =
          cursor.getString(cursor.getColumnIndex(TraktContract.Episodes.SCREEN));
      toWatchHolder.episodeBanner.setImage(bannerUrl);
    } else {
      toWatch.setVisibility(View.GONE);
      watchTitle.setVisibility(View.GONE);
      toWatchId = -1;
    }

    if (lastWatched != null) {
      if (cursor.moveToNext()) {
        lastWatched.setVisibility(View.VISIBLE);

        lastWatchedId = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));

        lastWatchedHolder.episodeTitle
            .setText(cursor.getString(cursor.getColumnIndex(TraktContract.Episodes.TITLE)));

        final long airTime =
            cursor.getLong(cursor.getColumnIndex(TraktContract.Episodes.FIRST_AIRED));
        final String airTimeStr = DateUtils.millisToString(getActivity(), airTime, false);
        lastWatchedHolder.episodeAirTime.setText(airTimeStr);

        final int season = cursor.getInt(cursor.getColumnIndex(TraktContract.Episodes.SEASON));
        final int episode = cursor.getInt(cursor.getColumnIndex(TraktContract.Episodes.EPISODE));
        lastWatchedHolder.episodeEpisode.setText("S" + season + "E" + episode);

        final String bannerUrl =
            cursor.getString(cursor.getColumnIndex(TraktContract.Episodes.SCREEN));
        lastWatchedHolder.episodeBanner.setImage(bannerUrl);
      } else {
        lastWatched.setVisibility(View.INVISIBLE);
        lastWatchedId = -1;
      }
    }

    if (toWatchId == -1 && lastWatchedId == -1 && toCollectId == -1 && lastCollectedId == -1) {
      episodes.setVisibility(View.GONE);
    } else {
      episodes.setVisibility(View.VISIBLE);
    }
  }

  private void updateEpisodeCollectViews(Cursor cursor) {
    if (cursor.moveToFirst()) {
      toCollect.setVisibility(View.VISIBLE);
      collectTitle.setVisibility(View.VISIBLE);

      toCollectId = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));

      toCollectHolder.episodeTitle
          .setText(cursor.getString(cursor.getColumnIndex(TraktContract.Episodes.TITLE)));

      final long airTime =
          cursor.getLong(cursor.getColumnIndex(TraktContract.Episodes.FIRST_AIRED));
      final String airTimeStr = DateUtils.millisToString(getActivity(), airTime, false);
      toCollectHolder.episodeAirTime.setText(airTimeStr);

      final int season = cursor.getInt(cursor.getColumnIndex(TraktContract.Episodes.SEASON));
      final int episode = cursor.getInt(cursor.getColumnIndex(TraktContract.Episodes.EPISODE));
      toCollectHolder.episodeEpisode.setText("S" + season + "E" + episode);

      final String bannerUrl =
          cursor.getString(cursor.getColumnIndex(TraktContract.Episodes.SCREEN));
      toCollectHolder.episodeBanner.setImage(bannerUrl);
    } else {
      toCollect.setVisibility(View.GONE);
      collectTitle.setVisibility(View.GONE);
      toCollectId = -1;
    }

    if (lastCollected != null) {
      if (cursor.moveToNext()) {
        lastCollected.setVisibility(View.VISIBLE);

        lastCollectedId = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));

        lastCollectedHolder.episodeTitle
            .setText(cursor.getString(cursor.getColumnIndex(TraktContract.Episodes.TITLE)));

        final long airTime =
            cursor.getLong(cursor.getColumnIndex(TraktContract.Episodes.FIRST_AIRED));
        final String airTimeStr = DateUtils.millisToString(getActivity(), airTime, false);
        lastCollectedHolder.episodeAirTime.setText(airTimeStr);

        final int season = cursor.getInt(cursor.getColumnIndex(TraktContract.Episodes.SEASON));
        final int episode = cursor.getInt(cursor.getColumnIndex(TraktContract.Episodes.EPISODE));
        lastCollectedHolder.episodeEpisode.setText("S" + season + "E" + episode);

        final String bannerUrl =
            cursor.getString(cursor.getColumnIndex(TraktContract.Episodes.SCREEN));
        lastCollectedHolder.episodeBanner.setImage(bannerUrl);
      } else {
        lastCollectedId = -1;
        lastCollected.setVisibility(View.INVISIBLE);
      }
    }

    if (toWatchId == -1 && lastWatchedId == -1 && toCollectId == -1 && lastCollectedId == -1) {
      episodes.setVisibility(View.GONE);
    } else {
      episodes.setVisibility(View.VISIBLE);
    }
  }

  private LoaderManager.LoaderCallbacks<Cursor> loaderCallbacks =
      new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
          CursorLoader cl =
              new CursorLoader(getActivity(), Shows.buildShowUri(showId), SHOW_PROJECTION, null,
                  null, null);
          cl.setUpdateThrottle(2 * android.text.format.DateUtils.SECOND_IN_MILLIS);
          return cl;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor data) {
          updateShowView(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
        }
      };

  private LoaderManager.LoaderCallbacks<Cursor> genreCallbacks =
      new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
          CursorLoader cl =
              new CursorLoader(getActivity(), TraktContract.ShowGenres.buildFromShowId(showId),
                  GENRES_PROJECTION, null, null, TraktContract.ShowGenres.DEFAULT_SORT);
          cl.setUpdateThrottle(2 * android.text.format.DateUtils.SECOND_IN_MILLIS);
          return cl;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor data) {
          updateGenreViews(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
        }
      };

  private LoaderManager.LoaderCallbacks<Cursor> episodeWatchCallbacks =
      new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
          return new WatchedLoader(getActivity(), showId);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
          updateEpisodeWatchViews(cursor);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
        }
      };

  private LoaderManager.LoaderCallbacks<Cursor> episodeCollectCallbacks =
      new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
          return new CollectLoader(getActivity(), showId);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
          updateEpisodeCollectViews(cursor);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
        }
      };

  private LoaderManager.LoaderCallbacks<Cursor> seasonsLoader =
      new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
          CursorLoader cl =
              new CursorLoader(getActivity(), TraktContract.Seasons.buildFromShowId(showId), SeasonsAdapter.PROJECTION,
                  null, null, TraktContract.Seasons.DEFAULT_SORT);
          cl.setUpdateThrottle(2 * android.text.format.DateUtils.SECOND_IN_MILLIS);
          return cl;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor data) {
          seasonsAdapter.changeCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
          seasonsAdapter.changeCursor(null);
        }
      };
}
