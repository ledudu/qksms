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
package org.goodev.rms.manager

import android.content.Context
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure
import org.goodev.rms.data.BuildConfig
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManagerImpl @Inject constructor(val context: Context) : AnalyticsManager {

    override fun init() {
        UMConfigure.init(context, BuildConfig.APP_KEY, BuildConfig.CHANNEL, UMConfigure.DEVICE_TYPE_PHONE, "")
        UMConfigure.setLogEnabled(BuildConfig.DEBUG)
        MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.AUTO)
    }

    override fun track(event: String, vararg properties: Pair<String, Any>) {
        val id = event.replace(" ", "_")
        if (properties.isNullOrEmpty()) {
            MobclickAgent.onEvent(context, id)
        } else {
            val propertiesMap: Map<String, Any> = properties
                    .associateBy { pair -> pair.first }
                    .mapValues { pair -> pair.value.second }
                    .also { Timber.v("$event: $it") }

            MobclickAgent.onEventObject(context, id, propertiesMap)
        }
    }

    override fun setUserProperty(key: String, value: Any) {
        Timber.v("$key: $value")

        // Set the value in Mixpanel
        val properties = JSONObject()
        properties.put(key, value)
//        mixpanel.registerSuperProperties(properties)

        // Set the value in Amplitude
//        val identify = Identify()
//        when (value) {
//            is Boolean -> identify.set(key, value)
//            is BooleanArray -> identify.set(key, value)
//            is Double -> identify.set(key, value)
//            is DoubleArray -> identify.set(key, value)
//            is Float -> identify.set(key, value)
//            is FloatArray -> identify.set(key, value)
//            is Int -> identify.set(key, value)
//            is IntArray -> identify.set(key, value)
//            is Long -> identify.set(key, value)
//            is LongArray -> identify.set(key, value)
//            is String -> identify.set(key, value)
//            is JSONArray -> identify.set(key, value)
//            is JSONObject -> identify.set(key, value)
//            else -> Timber.e("Value of type ${value::class.java} not supported")
//        }
//        amplitude.identify(identify)
    }

}
