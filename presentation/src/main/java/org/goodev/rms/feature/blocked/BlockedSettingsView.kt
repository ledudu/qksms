package org.goodev.rms.feature.blocked

import io.reactivex.Observable
import org.goodev.rms.common.base.QkView

/**
 * Created on 2019/4/22.
 */
interface BlockedSettingsView : QkView<BlockedSettingsState> {
    val addKeywordIntent: Observable<String>
    val deleteKeywordIntent: Observable<String>
}