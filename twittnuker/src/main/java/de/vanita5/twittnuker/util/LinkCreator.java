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

package de.vanita5.twittnuker.util;

import android.net.Uri;

import de.vanita5.twittnuker.Constants;

public class LinkCreator implements Constants {

	private static final String AUTHORITY_TWITTER = "twitter.com";

	public static Uri getStatusTwitterLink(String screenName, long statusId) {
		Uri.Builder builder = new Uri.Builder();
		builder.scheme(SCHEME_HTTPS);
		builder.authority(AUTHORITY_TWITTER);
		builder.appendPath(screenName);
		builder.appendPath("status");
		builder.appendPath(String.valueOf(statusId));
		return builder.build();
	}

	public static Uri getTwidereStatusLink(long accountId, long statusId) {
		final Uri.Builder builder = new Uri.Builder();
		builder.scheme(SCHEME_TWITTNUKER);
		builder.authority(AUTHORITY_STATUS);
		if (accountId > 0) {
			builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(accountId));
		}
		builder.appendQueryParameter(QUERY_PARAM_STATUS_ID, String.valueOf(statusId));
		return builder.build();
	}

	public static Uri getTwidereUserLink(long accountId, long userId, String screenName) {
		final Uri.Builder builder = new Uri.Builder();
		builder.scheme(SCHEME_TWITTNUKER);
		builder.authority(AUTHORITY_USER);
		if (accountId > 0) {
			builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(accountId));
		}
		if (userId > 0) {
			builder.appendQueryParameter(QUERY_PARAM_USER_ID, String.valueOf(userId));
		}
		if (screenName != null) {
			builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName);
		}
		return builder.build();
	}

	public static Uri getUserTwitterLink(String screenName) {
		Uri.Builder builder = new Uri.Builder();
		builder.scheme(SCHEME_HTTPS);
		builder.authority(AUTHORITY_TWITTER);
		builder.appendPath(screenName);
		return builder.build();
	}
}