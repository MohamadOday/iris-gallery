package com.iris.gallery.ui

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.iris.gallery.data.TimelineDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun rememberAppLocale(): Locale {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            configuration.locale
        } ?: Locale.getDefault()
    }
}

fun getTimelinePattern(
    format: TimelineDateFormat,
    isSameYear: Boolean,
    showDayOfWeek: Boolean,
    locale: Locale,
    customPattern: String = "d. MMMM yyyy",
): String {
    return when (format) {
        TimelineDateFormat.CUSTOM -> {
            val base = customPattern.ifBlank { "d. MMMM yyyy" }
            if (showDayOfWeek && !base.contains("E")) {
                "EEEE, $base"
            } else {
                base
            }
        }
        TimelineDateFormat.SYSTEM_DEFAULT -> {
            val skeleton = if (isSameYear) {
                if (showDayOfWeek) "EEEEMMMMd" else "MMMMd"
            } else {
                if (showDayOfWeek) "EEEEyMMMMd" else "yMMMMd"
            }
            runCatching {
                android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton)
            }.getOrElse {
                if (isSameYear) {
                    if (showDayOfWeek) "EEEE, MMMM d" else "MMMM d"
                } else {
                    if (showDayOfWeek) "EEEE, MMMM d, yyyy" else "MMMM d, yyyy"
                }
            }
        }
        TimelineDateFormat.DAY_MONTH_YEAR -> {
            if (isSameYear) {
                if (showDayOfWeek) "EEEE, d MMMM" else "d MMMM"
            } else {
                if (showDayOfWeek) "EEEE, d MMMM yyyy" else "d MMMM yyyy"
            }
        }
        TimelineDateFormat.MONTH_DAY_YEAR -> {
            if (isSameYear) {
                if (showDayOfWeek) "EEEE, MMMM d" else "MMMM d"
            } else {
                if (showDayOfWeek) "EEEE, MMMM d, yyyy" else "MMMM d, yyyy"
            }
        }
        TimelineDateFormat.YEAR_MONTH_DAY -> {
            if (isSameYear) {
                if (showDayOfWeek) "EEEE, MMMM d" else "MMMM d"
            } else {
                if (showDayOfWeek) "EEEE, yyyy MMMM d" else "yyyy MMMM d"
            }
        }
        TimelineDateFormat.NUMERIC_DMY -> {
            if (isSameYear) {
                if (showDayOfWeek) "EEE, dd/MM" else "dd/MM"
            } else {
                if (showDayOfWeek) "EEE, dd/MM/yyyy" else "dd/MM/yyyy"
            }
        }
        TimelineDateFormat.NUMERIC_MDY -> {
            if (isSameYear) {
                if (showDayOfWeek) "EEE, MM/dd" else "MM/dd"
            } else {
                if (showDayOfWeek) "EEE, MM/dd/yyyy" else "MM/dd/yyyy"
            }
        }
        TimelineDateFormat.NUMERIC_YMD -> {
            if (isSameYear) {
                if (showDayOfWeek) "EEE, MM-dd" else "MM-dd"
            } else {
                if (showDayOfWeek) "EEE, yyyy-MM-dd" else "yyyy-MM-dd"
            }
        }
    }
}

fun getTimelineFormatter(
    format: TimelineDateFormat,
    isSameYear: Boolean,
    showDayOfWeek: Boolean,
    locale: Locale,
    customPattern: String = "d. MMMM yyyy",
): DateTimeFormatter {
    val pattern = getTimelinePattern(format, isSameYear, showDayOfWeek, locale, customPattern)
    return runCatching {
        DateTimeFormatter.ofPattern(pattern, locale)
    }.getOrElse {
        DateTimeFormatter.ofPattern(if (isSameYear) "MMMM d" else "MMMM d, yyyy", locale)
    }
}

fun formatTimelineDate(
    date: LocalDate,
    today: LocalDate,
    sameYearFormatter: DateTimeFormatter,
    otherYearFormatter: DateTimeFormatter,
    useRelativeDates: Boolean,
    todayString: String,
    yesterdayString: String,
): String {
    if (useRelativeDates) {
        if (date == today) return todayString
        if (date == today.minusDays(1)) return yesterdayString
    }
    return date.format(if (date.year == today.year) sameYearFormatter else otherYearFormatter)
}
