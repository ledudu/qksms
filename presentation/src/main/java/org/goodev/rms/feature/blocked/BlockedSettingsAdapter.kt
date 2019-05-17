package org.goodev.rms.feature.blocked

import android.view.LayoutInflater
import android.view.ViewGroup
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.blocked_keyword_list_item.view.*
import org.goodev.rms.R
import org.goodev.rms.common.base.QkAdapter
import org.goodev.rms.common.base.QkViewHolder
import javax.inject.Inject

/**
 * Created on 2019/4/22.
 */
class BlockedSettingsAdapter @Inject constructor() : QkAdapter<String>() {
    val deleteKeyword: Subject<String> = PublishSubject.create()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.blocked_keyword_list_item, parent, false)
        return QkViewHolder(view).apply {
            view.delete.setOnClickListener { deleteKeyword.onNext(getItem(adapterPosition)) }
        }
    }

    override fun onBindViewHolder(holder: QkViewHolder, position: Int) {
        val text = getItem(position)
        val view = holder.containerView

        view.text.text = text
    }

}