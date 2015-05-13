/*
 * Copyright 2007 Yusuke Yamamoto
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.vanita5.twittnuker.api.twitter.api;

import org.mariotaku.simplerestapi.method.GET;

import de.vanita5.twittnuker.api.twitter.model.ResponseList;
import de.vanita5.twittnuker.api.twitter.model.SavedSearch;
import de.vanita5.twittnuker.api.twitter.model.TwitterException;

public interface SavedSearchesResources {
	SavedSearch createSavedSearch(String query) throws TwitterException;

	SavedSearch destroySavedSearch(int id) throws TwitterException;

    @GET("/saved_searches/list.json")
	ResponseList<SavedSearch> getSavedSearches() throws TwitterException;

	SavedSearch showSavedSearch(int id) throws TwitterException;
}