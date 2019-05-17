package org.goodev.rms.feature.blocked

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import dagger.android.AndroidInjection
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.blocked_activity.empty
import kotlinx.android.synthetic.main.blocked_settings_activity.*
import org.goodev.rms.R
import org.goodev.rms.common.base.QkThemedActivity
import org.goodev.rms.common.util.extensions.makeToast
import javax.inject.Inject

/**
 * Created on 2019/4/22.
 */
class BlockedSettingsActivity : QkThemedActivity(), BlockedSettingsView {
    @Inject
    lateinit var blockedAdapter: BlockedSettingsAdapter
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    override val addKeywordIntent: Subject<String> = PublishSubject.create()
    override val deleteKeywordIntent: Subject<String> by lazy { blockedAdapter.deleteKeyword }

    fun addKeyword(value: String) {
        keyword.setText("")
        if (value.isNullOrEmpty()) {
            makeToast(R.string.keyword_empty_tips)
        } else {
            addKeywordIntent.onNext(value)
        }
    }

    override fun render(state: BlockedSettingsState) {
        blockedAdapter.data = state.data
    }

    private val viewModel by lazy { ViewModelProviders.of(this, viewModelFactory)[BlockedSettingsViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.blocked_settings_activity)
        setTitle(R.string.settings_blocking_title)
        showBackButton(true)
        viewModel.bindView(this)

        blockedAdapter.emptyView = empty
        conversations.adapter = blockedAdapter
        addButton.setOnClickListener { addKeyword(keyword.text.toString()) }
        keyword.requestFocus()
    }
}