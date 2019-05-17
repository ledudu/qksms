package org.goodev.rms.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.goodev.rms.R
import org.goodev.rms.common.util.ClipboardUtils
import org.goodev.rms.common.util.extensions.makeToast

/**
 * Created on 2019/5/5.
 */
class CopyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val verifyCode = intent.getStringExtra("verifyCode")
        ClipboardUtils.copy(context, verifyCode)
        context.makeToast(R.string.toast_verify_code_copied)
    }
}