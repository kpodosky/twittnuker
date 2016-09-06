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

package de.vanita5.twittnuker.loader

import android.content.Context
import android.util.Log
import de.vanita5.twittnuker.library.MicroBlog
import de.vanita5.twittnuker.library.MicroBlogException
import de.vanita5.twittnuker.library.twitter.model.User
import de.vanita5.twittnuker.TwittnukerConstants
import de.vanita5.twittnuker.model.ListResponse
import de.vanita5.twittnuker.model.ParcelableCredentials
import de.vanita5.twittnuker.model.ParcelableUser
import de.vanita5.twittnuker.model.UserKey
import de.vanita5.twittnuker.model.util.ParcelableCredentialsUtils
import de.vanita5.twittnuker.model.util.ParcelableUserUtils
import de.vanita5.twittnuker.util.MicroBlogAPIFactory
import java.util.*

abstract class TwitterAPIUsersLoader(
        context: Context,
        val accountKey: UserKey?,
        data: List<ParcelableUser>?,
        fromUser: Boolean
) : ParcelableUsersLoader(context, data, fromUser) {

    override fun loadInBackground(): List<ParcelableUser> {
        if (accountKey == null) {
            return ListResponse.getListInstance<ParcelableUser>(MicroBlogException("No Account"))
        }
        val credentials = ParcelableCredentialsUtils.getCredentials(context,
                accountKey) ?: return ListResponse.getListInstance<ParcelableUser>(MicroBlogException("No Account"))
        val twitter = MicroBlogAPIFactory.getInstance(context, credentials, true,
                true) ?: return ListResponse.getListInstance<ParcelableUser>(MicroBlogException("No Account"))
        val data = data
        val users: List<User>
        try {
            users = getUsers(twitter, credentials)
        } catch (e: MicroBlogException) {
            Log.w(TwittnukerConstants.LOGTAG, e)
            return ListResponse.getListInstance(data)
        }

        var pos = data.size
        for (user in users) {
            if (hasId(user.id)) {
                continue
            }
            data.add(ParcelableUserUtils.fromUser(user, accountKey, pos.toLong()))
            pos++
        }
        Collections.sort(data)
        return ListResponse.getListInstance(data)
    }

    @Throws(MicroBlogException::class)
    protected abstract fun getUsers(twitter: MicroBlog,
                                    credentials: ParcelableCredentials): List<User>
}