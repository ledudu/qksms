package org.goodev.rms.feature.blocked

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.schedulers.Schedulers
import org.goodev.rms.common.androidxcompat.scope
import org.goodev.rms.common.base.QkViewModel
import org.goodev.rms.util.Preferences
import javax.inject.Inject

/**
 * Created on 2019/4/22.
 */
class BlockedSettingsViewModel @Inject constructor(
        private val context: Context,
        private var pref: Preferences) :
        QkViewModel<BlockedSettingsView, BlockedSettingsState>(BlockedSettingsState()) {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    init {
        val stringSet = prefs.getStringSet("keywords", emptySet())!!
        newState { copy(data = stringSet.toList()) }
    }

    override fun bindView(view: BlockedSettingsView) {
        super.bindView(view)
        view.addKeywordIntent
                .observeOn(Schedulers.io())
                .autoDisposable(view.scope())
                .subscribe { keyword -> saveKeyword(keyword) }
        view.deleteKeywordIntent
                .observeOn(Schedulers.io())
                .autoDisposable(view.scope())
                .subscribe { keyword -> deleteKeyword(keyword) }

    }

    private fun saveKeyword(keyword: String) {
        val set = prefs.getStringSet("keywords", emptySet())!!
        val newSet = mutableSetOf<String>()
        newSet.addAll(set)
        newSet.add(keyword)
        prefs.edit().putStringSet("keywords", newSet).apply()
        newState { copy(data = newSet.toList()) }
    }
    private fun deleteKeyword(keyword: String) {
        val set = prefs.getStringSet("keywords", emptySet())!!
        val newSet = mutableSetOf<String>()
        newSet.addAll(set)
        newSet.remove(keyword)
        prefs.edit().putStringSet("keywords", newSet).apply()
        newState { copy(data = newSet.toList()) }
    }
}