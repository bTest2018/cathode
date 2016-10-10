/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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

package net.simonvt.cathode.api.entity;

public class Ids {

  Long trakt;

  String slug;

  Long tvdb;

  String imdb;

  int tmdb;

  Long tvrage;

  public Long getTrakt() {
    return trakt;
  }

  public String getSlug() {
    return slug;
  }

  public Long getTvdb() {
    return tvdb;
  }

  public String getImdb() {
    return imdb;
  }

  public int getTmdb() {
    return tmdb;
  }

  public Long getTvrage() {
    return tvrage;
  }
}
