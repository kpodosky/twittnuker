/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2016 vanita5 <mail@vanit.as>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2016 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package de.vanita5.twittnuker.util;

import android.net.Uri;
import android.support.annotation.Nullable;

import java.util.List;

import static de.vanita5.twittnuker.TwittnukerConstants.QUERY_PARAM_EXTRA;

public class UriExtraUtils {
    private UriExtraUtils() {
    }

    @Nullable
    public static void addExtra(Uri.Builder builder, String key, Object value) {
        builder.appendQueryParameter(QUERY_PARAM_EXTRA, key + "=" + String.valueOf(value));
    }

    @Nullable
    public static String getExtra(Uri uri, String key) {
        return getExtra(uri.getQueryParameters(QUERY_PARAM_EXTRA), key);
    }

    @Nullable
    public static String getExtra(List<String> extras, String key) {
        for (String extra : extras) {
            final String prefix = key + "=";
            final int i = extra.indexOf(prefix);
            if (i == 0) {
                return extra.substring(prefix.length());
            }
        }
        return null;
    }

}