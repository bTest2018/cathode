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

package net.simonvt.cathode.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;
import java.util.ArrayList;
import java.util.List;

public class ObservableScrollView extends ScrollView {

  private List<ScrollListener> listeners = new ArrayList<>();

  public ObservableScrollView(Context context) {
    super(context);
  }

  public ObservableScrollView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ObservableScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override protected void onScrollChanged(int l, int t, int oldl, int oldt) {
    super.onScrollChanged(l, t, oldl, oldt);
    for (ScrollListener listener : listeners) {
      listener.onScrollChanged(l, t);
    }
  }

  public void addListener(ScrollListener listener) {
    listeners.add(listener);
  }

  public void removeListener(ScrollListener listener) {
    listeners.remove(listener);
  }

  public interface ScrollListener {

    void onScrollChanged(int l, int t);
  }
}