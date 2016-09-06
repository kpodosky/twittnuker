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
import android.support.v4.content.AsyncTaskLoader
import android.text.TextUtils

import de.vanita5.twittnuker.Constants
import de.vanita5.twittnuker.loader.iface.IExtendedLoader
import de.vanita5.twittnuker.model.ParcelableStatus
import de.vanita5.twittnuker.util.NoDuplicatesArrayList

abstract class ParcelableStatusesLoader(
        context: Context,
        adapterData: List<ParcelableStatus>?,
        protected val tabPosition: Int,
        override var fromUser: Boolean
) : AsyncTaskLoader<List<ParcelableStatus>>(context), Constants, IExtendedLoader {

    protected val data = NoDuplicatesArrayList<ParcelableStatus>()
    protected val isFirstLoad: Boolean

    init {
        isFirstLoad = adapterData == null
        if (adapterData != null) {
            data.addAll(adapterData)
        }
    }

    protected fun containsStatus(statusId: String): Boolean {
        for (status in this.data) {
            if (TextUtils.equals(status.id, statusId)) return true
        }
        return false
    }

    protected fun deleteStatus(statuses: MutableList<ParcelableStatus>?, statusId: String): Boolean {
        if (statuses == null || statuses.isEmpty()) return false
        var result = false
        for (i in statuses.indices.reversed()) {
            if (TextUtils.equals(statuses[i].id, statusId)) {
                statuses.removeAt(i)
                result = true
            }
        }
        return result
    }

    override fun onStartLoading() {
        forceLoad()
    }


}