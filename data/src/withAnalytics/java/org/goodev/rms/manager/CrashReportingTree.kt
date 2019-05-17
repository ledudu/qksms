package org.goodev.rms.manager

import android.content.Context
import android.util.Log
import com.umeng.analytics.MobclickAgent
import timber.log.Timber

/**
 * Created on 2019/4/19.
 */
class CrashReportingTree constructor(val context: Context) : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority == Log.WARN || priority == Log.ERROR
    }

    override fun log(priority: Int, tag: String?, message: String?, throwable: Throwable?) {
        throwable?.run { MobclickAgent.reportError(context, this) }
    }

}