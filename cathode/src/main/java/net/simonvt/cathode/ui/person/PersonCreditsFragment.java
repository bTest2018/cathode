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
package net.simonvt.cathode.ui.person;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import java.util.List;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.enumeration.Department;
import net.simonvt.cathode.common.ui.fragment.ToolbarGridFragment;
import net.simonvt.cathode.common.util.Ids;
import net.simonvt.cathode.common.util.guava.Preconditions;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.NavigationListener;

public class PersonCreditsFragment extends ToolbarGridFragment<PersonCreditsAdapter.ViewHolder>
    implements PersonCreditsAdapter.OnCreditClickListener {

  private static final String TAG = "net.simonvt.cathode.ui.person.CreditsFragment";

  private static final String ARG_PERSON_ID =
      "net.simonvt.cathode.ui.person.CreditsFragment.personId";

  private static final String ARG_DEPARTMENT =
      "net.simonvt.cathode.ui.person.CreditsFragment.department";

  private static final int LOADER_PERSON = 1;

  long personId;

  private Department department;

  private PersonCreditsAdapter adapter;

  private NavigationListener navigationListener;

  public static String getTag(long personId) {
    return TAG + "/" + personId + "/" + Ids.newId();
  }

  public static Bundle getArgs(long personId, Department department) {
    Preconditions.checkArgument(personId >= 0, "personId must be >= 0");

    Bundle args = new Bundle();
    args.putLong(ARG_PERSON_ID, personId);
    args.putSerializable(ARG_DEPARTMENT, department);
    return args;
  }

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationListener = (NavigationListener) activity;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    Bundle args = getArguments();
    personId = args.getLong(ARG_PERSON_ID);
    department = (Department) args.getSerializable(ARG_DEPARTMENT);

    switch (department) {
      case CAST:
        setTitle(R.string.person_department_cast);
        break;

      case PRODUCTION:
        setTitle(R.string.person_department_production);
        break;

      case ART:
        setTitle(R.string.person_department_art);
        break;

      case CREW:
        setTitle(R.string.person_department_crew);
        break;

      case COSTUME_AND_MAKEUP:
        setTitle(R.string.person_department_costume_makeup);
        break;

      case DIRECTING:
        setTitle(R.string.person_department_directing);
        break;

      case WRITING:
        setTitle(R.string.person_department_writing);
        break;

      case SOUND:
        setTitle(R.string.person_department_sound);
        break;

      case CAMERA:
        setTitle(R.string.person_department_camera);
        break;
    }

    getLoaderManager().initLoader(LOADER_PERSON, null, personLoader);
  }

  @Override protected int getColumnCount() {
    return getResources().getInteger(R.integer.personCreditColumns);
  }

  @Override public void onShowClicked(long showId, String title, String overview) {
    navigationListener.onDisplayShow(showId, title, overview, LibraryType.WATCHED);
  }

  @Override public void onMovieClicked(long movieId, String title, String overview) {
    navigationListener.onDisplayMovie(movieId, title, overview);
  }

  private void setCredits(List<PersonCredit> credits) {
    if (adapter == null) {
      adapter = new PersonCreditsAdapter(credits, this);
      setAdapter(adapter);
    } else {
      adapter.setCredits(credits);
    }
  }

  private LoaderManager.LoaderCallbacks<Person> personLoader =
      new LoaderManager.LoaderCallbacks<Person>() {
        @Override public Loader<Person> onCreateLoader(int id, Bundle args) {
          return new PersonLoader(getContext(), personId);
        }

        @Override public void onLoadFinished(Loader<Person> loader, Person data) {
          switch (department) {
            case CAST:
              setCredits(data.getCredits().getCast());
              break;

            case PRODUCTION:
              setCredits(data.getCredits().getProduction());
              break;

            case ART:
              setCredits(data.getCredits().getArt());
              break;

            case CREW:
              setCredits(data.getCredits().getCrew());
              break;

            case COSTUME_AND_MAKEUP:
              setCredits(data.getCredits().getCostumeAndMakeUp());
              break;

            case DIRECTING:
              setCredits(data.getCredits().getDirecting());
              break;

            case WRITING:
              setCredits(data.getCredits().getWriting());
              break;

            case SOUND:
              setCredits(data.getCredits().getSound());
              break;

            case CAMERA:
              setCredits(data.getCredits().getCamera());
              break;
          }
        }

        @Override public void onLoaderReset(Loader<Person> loader) {
        }
      };
}
