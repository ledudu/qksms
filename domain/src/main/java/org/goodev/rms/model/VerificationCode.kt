package org.goodev.rms.model

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import java.util.regex.Pattern

class VerificationCode(_value: String, _color: Int) {
    companion object {
        val codePattern: Pattern = Pattern.compile("([A-Za-z0-9]{4,})(?![\u5e74A-Za-z0-9])")
        val codeNumberPattern: Pattern = Pattern.compile("([0-9]{4,})(?![0-9])")
        val companyPattern: Pattern = Pattern.compile("(\u3010(.*?)\u3011)|(\\[(.*?)\\])")
        val namePattern: Pattern = Pattern.compile(".{2}\u7801")
    }

    // Verification SMS only
    var verifyCode: String = ""
    /// 随机码、验证码、注册码、认证码、
    var verifyName: String = ""
    var verifyCompany: String = ""
    var summary: CharSequence = ""
    var isVerifyCode: Boolean = false

    init {
        val nameMatcher = namePattern.matcher(_value);
        if (nameMatcher.find()) {
            verifyName = nameMatcher.group(0)
            val codeNumberMatcher = codeNumberPattern.matcher(_value)
            val codeMatcher = codePattern.matcher(_value)
            if (codeNumberMatcher.find()) {
                verifyCode = codeNumberMatcher.group(1)
            } else if (codeMatcher.find()) {
                verifyCode = codeMatcher.group(1)
            }
            val companyMatcher = companyPattern.matcher(_value)
            if (companyMatcher.find()) {
                verifyCompany = companyMatcher.group(2) ?: companyMatcher.group(4) ?: ""
            }
            isVerifyCode = verifyName.isNotEmpty() && verifyCode.isNotEmpty()
                    && !verifyName.contains("编码")// TODO 排除特殊的带 码 的短信

            val sb = SpannableStringBuilder("$verifyCompany - $verifyName - ")
            val fcs = ForegroundColorSpan(_color);
            val bss = StyleSpan(Typeface.BOLD);
            val index = sb.length
            sb.append(verifyCode)
            sb.setSpan(fcs, index, sb.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            sb.setSpan(bss, index, sb.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            summary = sb
        }

    }

}