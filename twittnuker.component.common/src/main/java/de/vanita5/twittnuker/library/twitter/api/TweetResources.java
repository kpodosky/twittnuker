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

package de.vanita5.twittnuker.library.twitter.api;

import de.vanita5.twittnuker.library.MicroBlogException;
import de.vanita5.twittnuker.library.twitter.model.IDs;
import de.vanita5.twittnuker.library.twitter.model.Paging;
import de.vanita5.twittnuker.library.twitter.model.ResponseList;
import de.vanita5.twittnuker.library.twitter.model.Status;
import de.vanita5.twittnuker.library.twitter.model.StatusUpdate;
import de.vanita5.twittnuker.library.twitter.template.StatusAnnotationTemplate;
import org.mariotaku.restfu.annotation.method.GET;
import org.mariotaku.restfu.annotation.method.POST;
import org.mariotaku.restfu.annotation.param.Param;
import org.mariotaku.restfu.annotation.param.Path;
import org.mariotaku.restfu.annotation.param.Queries;
import org.mariotaku.restfu.annotation.param.Query;

@SuppressWarnings("RedundantThrows")
@Queries(template = StatusAnnotationTemplate.class)
public interface TweetResources {
    @POST("/statuses/destroy/{id}.json")
    Status destroyStatus(@Path("id") String statusId) throws MicroBlogException;

    @GET("/statuses/retweeters/ids.json")
    IDs getRetweetersIDs(@Query("id") String statusId, @Query Paging paging) throws MicroBlogException;

    @GET("/statuses/retweets/{id}.json")
    ResponseList<Status> getRetweets(@Path("id") String statusId, @Query Paging paging) throws MicroBlogException;

    @POST("/statuses/retweet/{id}.json")
    Status retweetStatus(@Path("id") String statusId) throws MicroBlogException;

    @POST("/statuses/unretweet/{id}.json")
    Status unretweetStatus(@Path("id") String statusId) throws MicroBlogException;

    @GET("/statuses/show.json")
    Status showStatus(@Query("id") String id) throws MicroBlogException;

    @POST("/statuses/update.json")
    Status updateStatus(@Param StatusUpdate latestStatus) throws MicroBlogException;

    @POST("/statuses/lookup.json")
    ResponseList<Status> lookupStatuses(@Param(value = "id", arrayDelimiter = ',') String[] ids) throws MicroBlogException;

}