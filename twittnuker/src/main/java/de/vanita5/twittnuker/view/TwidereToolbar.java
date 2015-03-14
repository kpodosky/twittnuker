/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2015 vanita5 <mail@vanita5.de>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2015 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vanita5.twittnuker.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.util.ThemeUtils;

public class TwidereToolbar extends Toolbar {
	public TwidereToolbar(Context context) {
		super(context, null);
	}

	public TwidereToolbar(Context context, AttributeSet attrs) {
		this(context, attrs, R.attr.toolbarStyle);
	}

	public TwidereToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(getThemedContext(context, attrs, defStyleAttr), attrs, defStyleAttr);
        final TypedArray a = getContext().obtainStyledAttributes(attrs, new int[]{R.attr.elevation}, defStyleAttr, 0);
		ViewCompat.setElevation(this, a.getDimension(0, 0));
    }

    private static Context getThemedContext(Context context, AttributeSet attrs, int defStyleAttr) {
        return ThemeUtils.getActionBarContext(context);
	}
}