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

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v7.app.AlertDialog
import org.mariotaku.ktextension.toTypedArray
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.constant.IntentConstants.*
import de.vanita5.twittnuker.model.ParcelableMedia
import de.vanita5.twittnuker.model.ParcelableStatus
import de.vanita5.twittnuker.model.UserKey
import de.vanita5.twittnuker.util.IntentUtils

class SensitiveContentWarningDialogFragment : BaseDialogFragment(), DialogInterface.OnClickListener {

    override fun onClick(dialog: DialogInterface, which: Int) {
        when (which) {
            DialogInterface.BUTTON_POSITIVE -> {
                val context = activity
                val args = arguments
                if (args == null || context == null) return
                val accountKey = args.getParcelable<UserKey>(EXTRA_ACCOUNT_KEY)
                val current = args.getParcelable<ParcelableMedia>(EXTRA_CURRENT_MEDIA)
                val status = args.getParcelable<ParcelableStatus>(EXTRA_STATUS)
                val option = args.getBundle(EXTRA_ACTIVITY_OPTIONS)
                val newDocument = args.getBoolean(EXTRA_NEW_DOCUMENT)
                val media = args.getParcelableArray(EXTRA_MEDIA).toTypedArray(ParcelableMedia.CREATOR)
                IntentUtils.openMediaDirectly(context, accountKey, status, null, current, media,
                        option, newDocument)
            }
        }

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = activity
        val builder = AlertDialog.Builder(context)
        builder.setTitle(android.R.string.dialog_alert_title)
        builder.setMessage(R.string.sensitive_content_warning)
        builder.setPositiveButton(android.R.string.ok, this)
        builder.setNegativeButton(android.R.string.cancel, null)
        return builder.create()
    }

}