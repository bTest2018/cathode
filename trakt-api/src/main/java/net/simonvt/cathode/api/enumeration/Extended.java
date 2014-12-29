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
import java.util.Map;

public enum Extended {
  FULL("full"),
  IMAGES("images"),
  FULL_IMAGES("full,images");

  private final String value;

  private Extended(String value) {
    this.value = value;
  }

  @Override public String toString() {
    return value;
  }

  private static final Map<String, Extended> STRING_MAPPING = new HashMap<String, Extended>();

  static {
    for (Extended via : Extended.values()) {
      STRING_MAPPING.put(via.toString().toUpperCase(), via);
    }
  }

  public static Extended fromValue(String value) {
    return STRING_MAPPING.get(value.toUpperCase());
  }
}