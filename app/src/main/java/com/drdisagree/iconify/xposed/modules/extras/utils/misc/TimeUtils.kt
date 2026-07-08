package com.drdisagree.iconify.xposed.modules.extras.utils.misc

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.TextWatcher
import android.text.format.DateFormat
import android.text.style.ForegroundColorSpan
import android.widget.TextClock
import android.widget.TextView
import com.drdisagree.iconify.R
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeUtils {

    private fun getNumberString(modRes: Resources, index: Int): String {
        val numbers = modRes.getStringArray(R.array.numbers)
        return numbers.getOrElse(index) { modRes.getString(R.string.not_available) }
    }

    private fun getFuzzString(modRes: Resources, index: Int): String {
        val fuzz = modRes.getStringArray(R.array.fuzz)
        return fuzz.getOrElse(index) { modRes.getString(R.string.not_available) }
    }

    fun regionFormattedDate(usFormat: String?, euFormat: String?): String {
        try {
            val currentDate = Date()
            val currentLocale = Locale.getDefault()
            return if (currentLocale == Locale.US) {
                val usDateFormat = SimpleDateFormat(usFormat, Locale.US)
                usDateFormat.format(currentDate)
            } else {
                val euDateFormat = SimpleDateFormat(euFormat, currentLocale)
                euDateFormat.format(currentDate)
            }
        } catch (throwable: Throwable) {
            log(this@TimeUtils, throwable)
        }
        return SimpleDateFormat(usFormat, Locale.getDefault()).format(Date())
    }

    fun formatTime(context: Context, format24H: String, format12H: String): String {
        return formatTime(if (DateFormat.is24HourFormat(context)) format24H else format12H)
    }

    private fun formatTime(format: String): String {
        return SimpleDateFormat(format, Locale.getDefault()).format(Date())
    }

    fun setCurrentTimeTextClock(
        context: Context,
        modRes: Resources,
        tickIndicator: TextClock,
        hourView: TextView,
        minuteView: TextView
    ) {
        setCurrentTimeHour(context, modRes, hourView)
        setCurrentTimeMinute(modRes, minuteView)

        tickIndicator.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrEmpty()) {
                    setCurrentTimeHour(context, modRes, hourView)
                    setCurrentTimeMinute(modRes, minuteView)
                }
            }
        })
    }

    private fun setCurrentTimeHour(context: Context, modRes: Resources, hourView: TextView) {
        val hourFormat = if (DateFormat.is24HourFormat(context)) "HH" else "hh"
        val hour = SimpleDateFormat(
            hourFormat,
            Locale.getDefault()
        ).format(Calendar.getInstance().time)
        hourView.text = getNumberString(modRes, hour.toInt())
    }

    private fun setCurrentTimeMinute(modRes: Resources, minuteView: TextView) {
        val minuteFormat = "mm"
        val minute = SimpleDateFormat(
            minuteFormat,
            Locale.getDefault()
        ).format(Calendar.getInstance().time)
        minuteView.text = getNumberString(modRes, minute.toInt())
    }

    fun isSecurityPatchBefore(targetDate: Calendar): Boolean {
        val securityPatch = Build.VERSION.SECURITY_PATCH
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        return try {
            val securityPatchDate = dateFormat.parse(securityPatch)
            (securityPatchDate != null && (securityPatchDate < targetDate.time))
        } catch (e: Exception) {
            log(this@TimeUtils, "Error parsing security patch date\n$e")
            false
        }
    }

    fun isSecurityPatchAfter(targetDate: Calendar): Boolean {
        val securityPatch = Build.VERSION.SECURITY_PATCH
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        return try {
            val securityPatchDate = dateFormat.parse(securityPatch)
            (securityPatchDate != null && (securityPatchDate > targetDate.time))
        } catch (e: Exception) {
            log(this@TimeUtils, "Error parsing security patch date\n$e")
            false
        }
    }

    fun setCurrentTimeTextClockAccent(tickIndicator: TextClock?, hourView: TextView?, color: Int) {
        if (tickIndicator == null || hourView == null) return

        setCurrentTimeHourRed(tickIndicator, hourView, color)

        tickIndicator.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                if (!TextUtils.isEmpty(s)) {
                    setCurrentTimeHourRed(tickIndicator, hourView, color)
                }
            }
        })
    }

    private fun setCurrentTimeHourRed(tickIndicator: TextClock, hourView: TextView, color: Int) {
        val hourFormat = tickIndicator.text.toString()
        val sb = StringBuilder(hourFormat)
        val spannableString = SpannableString(sb)
        var i = 0
        while (i < 2 && i < sb.length) {
            if (sb[i] == '1') {
                spannableString.setSpan(
                    ForegroundColorSpan(color),
                    i,
                    i + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            i++
        }
        hourView.setText(spannableString, TextView.BufferType.SPANNABLE)
    }

    // New fuzzy time setter that uses getNumberString() and getFuzzString()
    fun setCurrentTimeFuzzy(
        context: Context,
        modRes: Resources,
        hourView: TextView,
        minuteView: TextView
    ) {
        val cal = Calendar.getInstance()
        val minute = cal.get(Calendar.MINUTE)
        // Use 12-hour display for fuzzy wording
        val hourRaw = cal.get(Calendar.HOUR) // 0..11
        var displayHour = if (hourRaw == 0) 12 else hourRaw // convert 0 -> 12
    
        // o'clock if within 2 minutes of the hour (<=2) or within last 2 minutes (>=58)
        if (minute <= 2 || minute >= 58) {
            minuteView.text = getFuzzString(modRes, 0) // "o'clock"
            hourView.text = getNumberString(modRes, displayHour)
            return
        }
    
        // Round to nearest 5 minutes (adding 2 -> rounds ties toward nearest)
        val rounded = ((minute + 2) / 5) * 5
        if (rounded == 60) {
            // Rounds up to the next hour and becomes o'clock
            displayHour = (displayHour % 12) + 1
            minuteView.text = getFuzzString(modRes, 0) // "o'clock"
            hourView.text = getNumberString(modRes, displayHour)
            return
        }
    
        // Map rounded minute to fuzz index:
        // 5 -> index 1, 10 -> 2, 15 -> 3, 20 -> 4, 25 -> 5, 30 -> 6
        fun fuzzIndexFor(mins: Int): Int = when (mins) {
            0 -> 0
            5 -> 1
            10 -> 2
            15 -> 3
            20 -> 4
            25 -> 5
            30 -> 6
            else -> 0
        }
    
        if (rounded <= 30) {
            val idx = fuzzIndexFor(rounded)
            val fuzz = getFuzzString(modRes, idx)
            minuteView.text = "$fuzz past"
            hourView.text = getNumberString(modRes, displayHour)
        } else {
            val minsTo = 60 - rounded
            val idx = fuzzIndexFor(minsTo)
            val fuzz = getFuzzString(modRes, idx)
            // roll hour forward for "to"
            displayHour = (displayHour % 12) + 1
            minuteView.text = "$fuzz to"
            hourView.text = getNumberString(modRes, displayHour)
        }
    }
    
    // Replace the calls in setCurrentTimeTextClock to use the fuzzy setter and keep the TextWatcher
    fun setCurrentTimeTextClockFuzzy(
        context: Context,
        modRes: Resources,
        tickIndicator: TextClock,
        hourView: TextView,
        minuteView: TextView
    ) {
        setCurrentTimeFuzzy(context, modRes, hourView, minuteView)
    
        tickIndicator.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrEmpty()) {
                    setCurrentTimeFuzzy(context, modRes, hourView, minuteView)
                }
            }
        })
    }
}
