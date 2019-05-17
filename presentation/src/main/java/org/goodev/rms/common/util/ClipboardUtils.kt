/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.goodev.rms.common.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import org.goodev.rms.R
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object ClipboardUtils {

    fun copy(context: Context, string: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("SMS", string)
        clipboard.primaryClip = clip
    }

    fun hasPrimaryClip(context: Context): Boolean {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return clipboard.hasPrimaryClip()
    }

    const val PREF_QUOTES = "quotes"
    const val KEY_QUOTES = "quotes_key"
    const val KEY_QUOTES_TIME = "quotes_time_key"
    const val QUOTES_COUNT = 228

    fun quoteText(context: Context): String {
        val preferences = context.getSharedPreferences(PREF_QUOTES, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val last = preferences.getLong(KEY_QUOTES_TIME, 0)
        var quote: String
        if (Math.abs(now - last) > TimeUnit.HOURS.toMillis(12) || !preferences.contains(KEY_QUOTES)) {
            val index = Random.nextInt(QUOTES_COUNT)
            quote = context.resources.getStringArray(R.array.quotes)[index]
            preferences.edit().putLong(KEY_QUOTES_TIME, now)
                    .putString(KEY_QUOTES, quote)
                    .apply()
        } else {
            quote = preferences.getString(KEY_QUOTES, "一寸光阴一寸金，寸金难买寸光阴。")!!
        }
        return quote
    }


}