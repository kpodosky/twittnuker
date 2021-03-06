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

package de.vanita5.twittnuker.task.twitter

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.support.annotation.UiThread
import android.util.Log
import com.squareup.otto.Bus
import org.mariotaku.abstask.library.AbstractTask
import de.vanita5.twittnuker.library.MicroBlog
import de.vanita5.twittnuker.library.MicroBlogException
import de.vanita5.twittnuker.library.twitter.model.Activity
import de.vanita5.twittnuker.library.twitter.model.Paging
import de.vanita5.twittnuker.library.twitter.model.ResponseList
import org.mariotaku.sqliteqb.library.Expression
import de.vanita5.twittnuker.BuildConfig
import de.vanita5.twittnuker.Constants
import de.vanita5.twittnuker.TwittnukerConstants.LOGTAG
import de.vanita5.twittnuker.TwittnukerConstants.QUERY_PARAM_NOTIFY
import de.vanita5.twittnuker.constant.SharedPreferenceConstants.KEY_LOAD_ITEM_LIMIT
import de.vanita5.twittnuker.model.ParcelableCredentials
import de.vanita5.twittnuker.model.RefreshTaskParam
import de.vanita5.twittnuker.model.UserKey
import de.vanita5.twittnuker.model.message.GetActivitiesTaskEvent
import de.vanita5.twittnuker.model.util.ParcelableActivityUtils
import de.vanita5.twittnuker.model.util.ParcelableCredentialsUtils
import de.vanita5.twittnuker.provider.TwidereDataStore.Activities
import de.vanita5.twittnuker.util.*
import de.vanita5.twittnuker.util.content.ContentResolverUtils
import de.vanita5.twittnuker.util.dagger.GeneralComponentHelper
import java.util.*
import javax.inject.Inject

abstract class GetActivitiesTask(protected val context: Context) : AbstractTask<RefreshTaskParam, Any, Any>(), Constants {
    @Inject
    lateinit var preferences: SharedPreferencesWrapper
    @Inject
    lateinit var bus: Bus
    @Inject
    lateinit var errorInfoStore: ErrorInfoStore
    @Inject
    lateinit var readStateManager: ReadStateManager
    @Inject
    lateinit var userColorNameManager: UserColorNameManager

    init {
        GeneralComponentHelper.build(context).inject(this)
    }

    public override fun doLongOperation(param: RefreshTaskParam): Any? {
        if (param.shouldAbort) return null
        val accountIds = param.accountKeys
        val maxIds = param.maxIds
        val maxSortIds = param.maxSortIds
        val sinceIds = param.sinceIds
        val cr = context.contentResolver
        val loadItemLimit = preferences.getInt(KEY_LOAD_ITEM_LIMIT)
        var saveReadPosition = false
        for (i in accountIds.indices) {
            val accountKey = accountIds[i]
            val noItemsBefore = DataStoreUtils.getActivitiesCount(context, contentUri,
                    accountKey) <= 0
            val credentials = ParcelableCredentialsUtils.getCredentials(context,
                    accountKey) ?: continue
            val twitter = MicroBlogAPIFactory.getInstance(context, credentials, true,
                    true) ?: continue
            val paging = Paging()
            paging.count(loadItemLimit)
            var maxId: String? = null
            var maxSortId: Long = -1
            if (maxIds != null) {
                maxId = maxIds[i]
                if (maxSortIds != null) {
                    maxSortId = maxSortIds[i]
                }
                if (maxId != null) {
                    paging.maxId(maxId)
                }
            }
            var sinceId: String? = null
            if (sinceIds != null) {
                sinceId = sinceIds[i]
                if (sinceId != null) {
                    paging.sinceId(sinceId)
                    if (maxIds == null || maxId == null) {
                        paging.setLatestResults(true)
                        saveReadPosition = true
                    }
                }
            }
            // We should delete old activities has intersection with new items
            try {
                val activities = getActivities(twitter, credentials, paging)
                storeActivities(cr, loadItemLimit, credentials, noItemsBefore, activities, sinceId,
                        maxId, false)
                if (saveReadPosition) {
                    saveReadPosition(accountKey, credentials, twitter)
                }
                errorInfoStore.remove(errorInfoKey, accountKey)
            } catch (e: MicroBlogException) {
                if (BuildConfig.DEBUG) {
                    Log.w(LOGTAG, e)
                }
                if (e.errorCode == 220) {
                    errorInfoStore.put(errorInfoKey, accountKey,
                            ErrorInfoStore.CODE_NO_ACCESS_FOR_CREDENTIALS)
                } else if (e.isCausedByNetworkIssue) {
                    errorInfoStore.put(errorInfoKey, accountKey,
                            ErrorInfoStore.CODE_NETWORK_ERROR)
                }
            }

        }
        return null
    }

    protected abstract val errorInfoKey: String

    private fun storeActivities(cr: ContentResolver, loadItemLimit: Int, credentials: ParcelableCredentials,
                                noItemsBefore: Boolean, activities: ResponseList<Activity>,
                                sinceId: String?, maxId: String?, notify: Boolean) {
        val deleteBound = LongArray(2, { return@LongArray -1 })
        val valuesList = ArrayList<ContentValues>()
        var minIdx = -1
        var minPositionKey: Long = -1
        if (!activities.isEmpty()) {
            val firstSortId = activities.first().createdAt.time
            val lastSortId = activities.last().createdAt.time
            // Get id diff of first and last item
            val sortDiff = firstSortId - lastSortId
            for (i in activities.indices) {
                val item = activities[i]
                val activity = ParcelableActivityUtils.fromActivity(item,
                        credentials.account_key, false)
                activity.position_key = GetStatusesTask.getPositionKey(activity.timestamp,
                        activity.timestamp, lastSortId, sortDiff, i, activities.size)
                if (deleteBound[0] < 0) {
                    deleteBound[0] = activity.min_sort_position
                } else {
                    deleteBound[0] = Math.min(deleteBound[0], activity.min_sort_position)
                }
                if (deleteBound[1] < 0) {
                    deleteBound[1] = activity.max_sort_position
                } else {
                    deleteBound[1] = Math.max(deleteBound[1], activity.max_sort_position)
                }
                if (minIdx == -1 || item < activities[minIdx]) {
                    minIdx = i
                    minPositionKey = activity.position_key
                }

                activity.inserted_date = System.currentTimeMillis()
                val values = ContentValuesCreator.createActivity(activity,
                        credentials, userColorNameManager)
                valuesList.add(values)
            }
        }
        var olderCount = -1
        if (minPositionKey > 0) {
            olderCount = DataStoreUtils.getActivitiesCount(context, contentUri, minPositionKey,
                    Activities.POSITION_KEY, false, credentials.account_key)
        }
        val writeUri = UriUtils.appendQueryParameters(contentUri, QUERY_PARAM_NOTIFY, notify)
        if (deleteBound[0] > 0 && deleteBound[1] > 0) {
            val where = Expression.and(
                    Expression.equalsArgs(Activities.ACCOUNT_KEY),
                    Expression.greaterEqualsArgs(Activities.MIN_SORT_POSITION),
                    Expression.lesserEqualsArgs(Activities.MAX_SORT_POSITION))
            val whereArgs = arrayOf(credentials.account_key.toString(), deleteBound[0].toString(), deleteBound[1].toString())
            val rowsDeleted = cr.delete(writeUri, where.sql, whereArgs)
            // Why loadItemLimit / 2? because it will not acting strange in most cases
            val insertGap = !noItemsBefore && olderCount > 0  && rowsDeleted <= 0 && activities.size > loadItemLimit / 2
            if (insertGap && !valuesList.isEmpty()) {
                valuesList[valuesList.size - 1].put(Activities.IS_GAP, true)
            }
        }
        ContentResolverUtils.bulkInsert(cr, writeUri, valuesList)

        if (maxId != null && sinceId == null) {
            val noGapValues = ContentValues()
            noGapValues.put(Activities.IS_GAP, false)
            val noGapWhere = Expression.and(Expression.equalsArgs(Activities.ACCOUNT_KEY),
                    Expression.equalsArgs(Activities.MIN_REQUEST_POSITION),
                    Expression.equalsArgs(Activities.MAX_REQUEST_POSITION)).sql
            val noGapWhereArgs = arrayOf(credentials.account_key.toString(), maxId, maxId)
            cr.update(writeUri, noGapValues, noGapWhere, noGapWhereArgs)
        }
    }

    protected abstract fun saveReadPosition(accountId: UserKey,
                                            credentials: ParcelableCredentials, twitter: MicroBlog)

    @Throws(MicroBlogException::class)
    protected abstract fun getActivities(twitter: MicroBlog,
                                         credentials: ParcelableCredentials,
                                         paging: Paging): ResponseList<Activity>

    public override fun afterExecute(handler: Any?, result: Any?) {
        context.contentResolver.notifyChange(contentUri, null)
        bus.post(GetActivitiesTaskEvent(contentUri, false, null))
    }

    protected abstract val contentUri: Uri

    @UiThread
    public override fun beforeExecute() {
        bus.post(GetActivitiesTaskEvent(contentUri, true, null))
    }
}