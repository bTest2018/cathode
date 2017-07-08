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

package net.simonvt.cathode.api.enumeration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum Gender {
  MALE("male"),
  FEMALE("female");

  private String gender;

  Gender(String gender) {
    this.gender = gender;
  }

  private static final Map<String, Gender> STRING_MAPPING = new HashMap<>();

  static {
    for (Gender via : Gender.values()) {
      STRING_MAPPING.put(via.toString().toUpperCase(Locale.US), via);
    }
  }

  public static Gender fromValue(String value) {
    return STRING_MAPPING.get(value.toUpperCase(Locale.US));
  }

  @Override public String toString() {
    return gender;
  }
}
