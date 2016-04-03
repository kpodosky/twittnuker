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

package de.vanita5.twittnuker.task.twitter;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.squareup.otto.Bus;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.mariotaku.abstask.library.AbstractTask;
import org.mariotaku.abstask.library.TaskStarter;
import org.mariotaku.sqliteqb.library.Columns;
import org.mariotaku.sqliteqb.library.Expression;
import de.vanita5.twittnuker.BuildConfig;
import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.api.twitter.Twitter;
import de.vanita5.twittnuker.api.twitter.TwitterException;
import de.vanita5.twittnuker.api.twitter.model.Paging;
import de.vanita5.twittnuker.api.twitter.model.ResponseList;
import de.vanita5.twittnuker.api.twitter.model.Status;
import de.vanita5.twittnuker.model.ParcelableCredentials;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.model.ParcelableStatusValuesCreator;
import de.vanita5.twittnuker.model.RefreshTaskParam;
import de.vanita5.twittnuker.model.UserKey;
import de.vanita5.twittnuker.model.message.GetStatusesTaskEvent;
import de.vanita5.twittnuker.model.util.ParcelableCredentialsUtils;
import de.vanita5.twittnuker.model.util.ParcelableStatusUtils;
import de.vanita5.twittnuker.provider.TwidereDataStore.AccountSupportColumns;
import de.vanita5.twittnuker.provider.TwidereDataStore.Statuses;
import de.vanita5.twittnuker.task.CacheUsersStatusesTask;
import de.vanita5.twittnuker.util.AsyncTwitterWrapper;
import de.vanita5.twittnuker.util.DataStoreUtils;
import de.vanita5.twittnuker.util.ErrorInfoStore;
import de.vanita5.twittnuker.util.InternalTwitterContentUtils;
import de.vanita5.twittnuker.util.SharedPreferencesWrapper;
import de.vanita5.twittnuker.util.TwitterAPIFactory;
import de.vanita5.twittnuker.util.TwitterWrapper;
import de.vanita5.twittnuker.util.UriUtils;
import de.vanita5.twittnuker.util.UserColorNameManager;
import de.vanita5.twittnuker.util.content.ContentResolverUtils;
import de.vanita5.twittnuker.util.dagger.GeneralComponentHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public abstract class GetStatusesTask extends AbstractTask<RefreshTaskParam,
        List<TwitterWrapper.StatusListResponse>, Object> implements Constants {

    protected final Context context;
    @Inject
    protected SharedPreferencesWrapper preferences;
    @Inject
    protected Bus bus;
    @Inject
    protected ErrorInfoStore errorInfoStore;
    @Inject
    protected UserColorNameManager manager;

    public GetStatusesTask(Context context) {
        this.context = context;
        GeneralComponentHelper.build(context).inject(this);
    }

    @NonNull
    public abstract ResponseList<Status> getStatuses(Twitter twitter, Paging paging)
            throws TwitterException;

    @NonNull
    protected abstract Uri getContentUri();


    @Override
    public void afterExecute(List<TwitterWrapper.StatusListResponse> result) {
        context.getContentResolver().notifyChange(getContentUri(), null);
        bus.post(new GetStatusesTaskEvent(getContentUri(), false, AsyncTwitterWrapper.getException(result)));
    }

    @Override
    protected void beforeExecute() {
        bus.post(new GetStatusesTaskEvent(getContentUri(), true, null));
    }

    @Override
    public List<TwitterWrapper.StatusListResponse> doLongOperation(final RefreshTaskParam param) {
        if (param.shouldAbort()) return Collections.emptyList();
        final UserKey[] accountKeys = param.getAccountKeys();
        final String[] maxIds = param.getMaxIds();
        final String[] sinceIds = param.getSinceIds();
        final long[] maxSortIds = param.getMaxSortIds();
        final long[] sinceSortIds = param.getSinceSortIds();
        final List<TwitterWrapper.StatusListResponse> result = new ArrayList<>();
        int idx = 0;
        final int loadItemLimit = preferences.getInt(KEY_LOAD_ITEM_LIMIT, DEFAULT_LOAD_ITEM_LIMIT);
        for (final UserKey accountKey : accountKeys) {
            final ParcelableCredentials credentials = ParcelableCredentialsUtils.getCredentials(context,
                    accountKey);
            if (credentials == null) continue;
            final Twitter twitter = TwitterAPIFactory.getTwitterInstance(context, credentials,
                    true, true);
            if (twitter == null) continue;
            try {
                final Paging paging = new Paging();
                paging.count(loadItemLimit);
                final String maxId, sinceId;
                long maxSortId = -1, sinceSortId = -1;
                if (maxIds != null && maxIds[idx] != null) {
                    maxId = maxIds[idx];
                    paging.maxId(maxId);
                    if (maxSortIds != null) {
                        maxSortId = maxSortIds[idx];
                    }
                } else {
                    maxSortId = -1;
                    maxId = null;
                }
                if (sinceIds != null && sinceIds[idx] != null) {
                    sinceId = sinceIds[idx];
                    long sinceIdLong = NumberUtils.toLong(sinceId, -1);
                    //TODO handle non-twitter case
                    if (sinceIdLong != -1) {
                        paging.sinceId(String.valueOf(sinceIdLong - 1));
                    } else {
                        paging.sinceId(sinceId);
                    }
                    if (sinceSortIds != null) {
                        sinceSortId = sinceSortIds[idx];
                    }
                    if (maxIds == null || sinceIds[idx] == null) {
                        paging.setLatestResults(true);
                    }
                } else {
                    sinceId = null;
                }
                final List<Status> statuses = getStatuses(twitter, paging);
                InternalTwitterContentUtils.getStatusesWithQuoteData(twitter, statuses);
                storeStatus(accountKey, credentials, statuses, sinceId, maxId, sinceSortId,
                        maxSortId, loadItemLimit, false);
                // TODO cache related data and preload
                final CacheUsersStatusesTask cacheTask = new CacheUsersStatusesTask(context);
                cacheTask.setParams(new TwitterWrapper.StatusListResponse(accountKey, statuses));
                TaskStarter.execute(cacheTask);
                errorInfoStore.remove(getErrorInfoKey(), accountKey.getId());
            } catch (final TwitterException e) {
                if (BuildConfig.DEBUG) {
                    Log.w(LOGTAG, e);
                }
                if (e.isCausedByNetworkIssue()) {
                    errorInfoStore.put(getErrorInfoKey(), accountKey.getId(),
                            ErrorInfoStore.CODE_NETWORK_ERROR);
                }
                result.add(new TwitterWrapper.StatusListResponse(accountKey, e));
            }
            idx++;
        }
        return result;
    }

    @NonNull
    protected abstract String getErrorInfoKey();

    private void storeStatus(@NonNull final UserKey accountKey, ParcelableCredentials credentials,
                             @NonNull final List<Status> statuses,
                             final String sinceId, final String maxId,
                             final long sinceSortId, final long maxSortId,
                             int loadItemLimit, final boolean notify) {
        final Uri uri = getContentUri();
        final Uri writeUri = UriUtils.appendQueryParameters(uri, QUERY_PARAM_NOTIFY, notify);
        final ContentResolver resolver = context.getContentResolver();
        final boolean noItemsBefore = DataStoreUtils.getStatusCount(context, uri, accountKey) <= 0;
        final ContentValues[] values = new ContentValues[statuses.size()];
        final String[] statusIds = new String[statuses.size()];
        int minIdx = -1;
        long minPositionKey = -1;
        boolean hasIntersection = false;
        if (!statuses.isEmpty()) {
            final long firstSortId = statuses.get(0).getSortId();
            final long lastSortId = statuses.get(statuses.size() - 1).getSortId();
            // Get id diff of first and last item
            long sortDiff = firstSortId - lastSortId;

            for (int i = 0, j = statuses.size(); i < j; i++) {
                final Status item = statuses.get(i);
                final ParcelableStatus status = ParcelableStatusUtils.fromStatus(item, accountKey,
                        false);
                ParcelableStatusUtils.updateExtraInformation(status, credentials, manager);
                status.position_key = getPositionKey(status.timestamp, status.sort_id, lastSortId,
                        sortDiff, i, j);
                status.inserted_date = System.currentTimeMillis();
                values[i] = ParcelableStatusValuesCreator.create(status);
                if (minIdx == -1 || item.compareTo(statuses.get(minIdx)) < 0) {
                    minIdx = i;
                    minPositionKey = status.position_key;
                }
                if (sinceId != null && item.getSortId() <= sinceSortId) {
                    hasIntersection = true;
                }
                statusIds[i] = item.getId();
            }
        }
        // Delete all rows conflicting before new data inserted.
        final Expression accountWhere = Expression.equalsArgs(AccountSupportColumns.ACCOUNT_KEY);
        final Expression statusWhere = Expression.inArgs(new Columns.Column(Statuses.STATUS_ID),
                statusIds.length);
        final String deleteWhere = Expression.and(accountWhere, statusWhere).getSQL();
        final String[] deleteWhereArgs = new String[statusIds.length + 1];
        System.arraycopy(statusIds, 0, deleteWhereArgs, 1, statusIds.length);
        deleteWhereArgs[0] = accountKey.toString();
        int olderCount = -1;
        if (minPositionKey > 0) {
            olderCount = DataStoreUtils.getStatusesCount(context, uri, minPositionKey,
                    Statuses.POSITION_KEY, false, accountKey);
        }
        final int rowsDeleted = resolver.delete(writeUri, deleteWhere, deleteWhereArgs);

        // Insert a gap.
        final boolean deletedOldGap = rowsDeleted > 0 && ArrayUtils.contains(statusIds, maxId);
        final boolean noRowsDeleted = rowsDeleted == 0;
        // Why loadItemLimit / 2? because it will not acting strange in most cases
        final boolean insertGap = minIdx != -1 && olderCount > 0 && (noRowsDeleted || deletedOldGap)
                && !noItemsBefore && !hasIntersection && statuses.size() > loadItemLimit / 2;
        if (insertGap) {
            values[minIdx].put(Statuses.IS_GAP, true);
        }
        // Insert previously fetched items.
        ContentResolverUtils.bulkInsert(resolver, writeUri, values);

        if (maxId != null && sinceId == null) {
            final ContentValues noGapValues = new ContentValues();
            noGapValues.put(Statuses.IS_GAP, false);
            final String noGapWhere = Expression.and(Expression.equalsArgs(Statuses.ACCOUNT_KEY),
                    Expression.equalsArgs(Statuses.STATUS_ID)).getSQL();
            final String[] noGapWhereArgs = {accountKey.toString(), maxId};
            resolver.update(writeUri, noGapValues, noGapWhere, noGapWhereArgs);
        }
    }

    public static long getPositionKey(long timestamp, long sortId, long lastSortId, long sortDiff,
                                      int position, int count) {
        if (sortDiff == 0) return timestamp;
        int extraValue;
        if (sortDiff > 0) {
            // descent sorted by time
            extraValue = count - 1 - position;
        } else {
            // ascent sorted by time
            extraValue = position;
        }
        return timestamp + (sortId - lastSortId) * (499 - count) / sortDiff + extraValue;
    }

}