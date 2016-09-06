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

package de.vanita5.twittnuker.fragment

import android.content.Context
import android.os.Bundle
import android.support.v4.app.LoaderManager.LoaderCallbacks
import android.support.v4.content.Loader
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.apache.commons.lang3.ArrayUtils
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.adapter.StaggeredGridParcelableStatusesAdapter
import de.vanita5.twittnuker.adapter.iface.ILoadMoreSupportAdapter
import de.vanita5.twittnuker.constant.IntentConstants.*
import de.vanita5.twittnuker.loader.MediaTimelineLoader
import de.vanita5.twittnuker.loader.iface.IExtendedLoader
import de.vanita5.twittnuker.model.ParcelableMedia
import de.vanita5.twittnuker.model.ParcelableStatus
import de.vanita5.twittnuker.model.UserKey
import de.vanita5.twittnuker.util.IntentUtils
import de.vanita5.twittnuker.view.HeaderDrawerLayout.DrawerCallback
import de.vanita5.twittnuker.view.holder.GapViewHolder
import de.vanita5.twittnuker.view.holder.iface.IStatusViewHolder

class UserMediaTimelineFragment : AbsContentRecyclerViewFragment<StaggeredGridParcelableStatusesAdapter, StaggeredGridLayoutManager>(), LoaderCallbacks<List<ParcelableStatus>>, DrawerCallback, IStatusViewHolder.StatusClickListener {

    override fun scrollToPositionWithOffset(position: Int, offset: Int) {
        layoutManager!!.scrollToPositionWithOffset(position, offset)
    }

    override var refreshing: Boolean
        get() {
            if (context == null || isDetached) return false
            return loaderManager.hasRunningLoaders()
        }
        set(value) {
            super.refreshing = value
        }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val adapter = adapter
        adapter!!.statusClickListener = this
        val loaderArgs = Bundle(arguments)
        loaderArgs.putBoolean(EXTRA_FROM_USER, true)
        loaderManager.initLoader(0, loaderArgs, this)
        showProgress()
    }

    override fun setupRecyclerView(context: Context, recyclerView: RecyclerView) {

    }

    override fun onCreateLayoutManager(context: Context): StaggeredGridLayoutManager {
        return StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
    }


    fun getStatuses(maxId: String?, sinceId: String?): Int {
        if (context == null) return -1
        val args = Bundle(arguments)
        args.putBoolean(EXTRA_MAKE_GAP, false)
        args.putString(EXTRA_MAX_ID, maxId)
        args.putString(EXTRA_SINCE_ID, sinceId)
        args.putBoolean(EXTRA_FROM_USER, true)
        loaderManager.restartLoader(0, args, this)
        return 0
    }


    override fun onCreateAdapter(context: Context): StaggeredGridParcelableStatusesAdapter {
        return StaggeredGridParcelableStatusesAdapter(context)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_content_recyclerview, container, false)
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<List<ParcelableStatus>> {
        val context = activity
        val accountKey = args.getParcelable<UserKey>(EXTRA_ACCOUNT_KEY)
        val maxId = args.getString(EXTRA_MAX_ID)
        val sinceId = args.getString(EXTRA_SINCE_ID)
        val userKey = args.getParcelable<UserKey>(EXTRA_USER_KEY)
        val screenName = args.getString(EXTRA_SCREEN_NAME)
        val tabPosition = args.getInt(EXTRA_TAB_POSITION, -1)
        val fromUser = args.getBoolean(EXTRA_FROM_USER)
        val loadingMore = args.getBoolean(EXTRA_LOADING_MORE, false)
        return MediaTimelineLoader(context, accountKey, userKey, screenName, sinceId, maxId,
                adapter!!.getData(), null, tabPosition, fromUser, loadingMore)
    }

    override fun onLoadFinished(loader: Loader<List<ParcelableStatus>>, data: List<ParcelableStatus>?) {
        val adapter = adapter
        val changed = adapter!!.setData(data)
        if ((loader as IExtendedLoader).fromUser && loader is MediaTimelineLoader) {
            val maxId = loader.maxId
            val sinceId = loader.sinceId
            if (TextUtils.isEmpty(sinceId) && !TextUtils.isEmpty(maxId)) {
                if (data != null && !data.isEmpty()) {
                    adapter.loadMoreSupportedPosition = if (changed) ILoadMoreSupportAdapter.END else ILoadMoreSupportAdapter.NONE
                }
            } else {
                adapter.loadMoreSupportedPosition = ILoadMoreSupportAdapter.END
            }
        }
        loader.fromUser = false
        showContent()
        setLoadMoreIndicatorPosition(ILoadMoreSupportAdapter.NONE)
    }

    override fun onLoaderReset(loader: Loader<List<ParcelableStatus>>) {
        adapter!!.setData(null)
    }

    override val reachingEnd: Boolean
        get() {
            val lm = layoutManager
            return ArrayUtils.contains(lm!!.findLastCompletelyVisibleItemPositions(null), lm.itemCount - 1)
        }

    override val reachingStart: Boolean
        get() {
            val lm = layoutManager
            return ArrayUtils.contains(lm!!.findFirstCompletelyVisibleItemPositions(null), 0)
        }

    override fun onLoadMoreContents(position: Long) {
        // Only supports load from end, skip START flag
        if (position and ILoadMoreSupportAdapter.START != 0L) return
        super.onLoadMoreContents(position.toLong())
        if (position == 0L) return
        val adapter = adapter ?: return
        val maxId = adapter.getStatusId(adapter.statusCount - 1)
        getStatuses(maxId, null)
    }


    override fun onMediaClick(holder: IStatusViewHolder, view: View, media: ParcelableMedia, statusPosition: Int) {

    }

    override fun onStatusClick(holder: IStatusViewHolder, position: Int) {
        IntentUtils.openStatus(context, adapter!!.getStatus(position)!!, null)
    }

    override fun onStatusLongClick(holder: IStatusViewHolder, position: Int): Boolean {
        return false
    }

    override fun onUserProfileClick(holder: IStatusViewHolder, position: Int) {

    }

    override fun onItemActionClick(holder: RecyclerView.ViewHolder, id: Int, position: Int) {

    }

    override fun onItemMenuClick(holder: RecyclerView.ViewHolder, menuView: View, position: Int) {

    }

    override fun onGapClick(holder: GapViewHolder, position: Int) {

    }
}