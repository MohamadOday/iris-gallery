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

fun stripYearFromPattern(pattern: String): String {
    return runCatching {
        pattern
            .replace(Regex("[,/.\\-]?\\s*y+\\s*[,/.\\-]?"), "")
            .replace(Regex("^[,/.\\-]\\s*|\\s*[,/.\\-]\$"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }.getOrDefault(pattern)
}

fun getTimelinePattern(
    format: TimelineDateFormat,
    isSameYear: Boolean,
    showDayOfWeek: Boolean,
    locale: Locale,
    customPattern: String = "d. MMMM yyyy",
    abbreviateDayOfWeek: Boolean = false,
    smartYearHiding: Boolean = true,
): String {
    val dayToken = if (abbreviateDayOfWeek) "EEE" else "EEEE"
    val shouldHideYear = isSameYear && smartYearHiding

    return when (format) {
        TimelineDateFormat.CUSTOM -> {
            val base = customPattern.ifBlank { "d. MMMM yyyy" }
            val withDay = if (showDayOfWeek && !base.contains("E")) {
                "$dayToken, $base"
            } else {
                base
            }
            if (shouldHideYear) stripYearFromPattern(withDay).ifBlank { withDay } else withDay
        }
        TimelineDateFormat.SYSTEM_DEFAULT -> {
            val skeleton = if (shouldHideYear) {
                if (showDayOfWeek) "${dayToken}MMMMd" else "MMMMd"
            } else {
                if (showDayOfWeek) "${dayToken}yMMMMd" else "yMMMMd"
            }
            runCatching {
                android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton)
            }.getOrElse {
                if (shouldHideYear) {
                    if (showDayOfWeek) "$dayToken, MMMM d" else "MMMM d"
                } else {
                    if (showDayOfWeek) "$dayToken, MMMM d, yyyy" else "MMMM d, yyyy"
                }
            }
        }
        TimelineDateFormat.DAY_MONTH_YEAR -> {
            if (shouldHideYear) {
                if (showDayOfWeek) "$dayToken, d MMMM" else "d MMMM"
            } else {
                if (showDayOfWeek) "$dayToken, d MMMM yyyy" else "d MMMM yyyy"
            }
        }
        TimelineDateFormat.MONTH_DAY_YEAR -> {
            if (shouldHideYear) {
                if (showDayOfWeek) "$dayToken, MMMM d" else "MMMM d"
            } else {
                if (showDayOfWeek) "$dayToken, MMMM d, yyyy" else "MMMM d, yyyy"
            }
        }
        TimelineDateFormat.YEAR_MONTH_DAY -> {
            if (shouldHideYear) {
                if (showDayOfWeek) "$dayToken, MMMM d" else "MMMM d"
            } else {
                if (showDayOfWeek) "$dayToken, yyyy MMMM d" else "yyyy MMMM d"
            }
        }
        TimelineDateFormat.NUMERIC_DMY -> {
            if (shouldHideYear) {
                if (showDayOfWeek) "$dayToken, dd/MM" else "dd/MM"
            } else {
                if (showDayOfWeek) "$dayToken, dd/MM/yyyy" else "dd/MM/yyyy"
            }
        }
        TimelineDateFormat.NUMERIC_MDY -> {
            if (shouldHideYear) {
                if (showDayOfWeek) "$dayToken, MM/dd" else "MM/dd"
            } else {
                if (showDayOfWeek) "$dayToken, MM/dd/yyyy" else "MM/dd/yyyy"
            }
        }
        TimelineDateFormat.NUMERIC_YMD -> {
            if (shouldHideYear) {
                if (showDayOfWeek) "$dayToken, MM-dd" else "MM-dd"
            } else {
                if (showDayOfWeek) "$dayToken, yyyy-MM-dd" else "yyyy-MM-dd"
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
    abbreviateDayOfWeek: Boolean = false,
    smartYearHiding: Boolean = true,
): DateTimeFormatter {
    val pattern = getTimelinePattern(format, isSameYear, showDayOfWeek, locale, customPattern, abbreviateDayOfWeek, smartYearHiding)
    return runCatching {
        val formatter = DateTimeFormatter.ofPattern(pattern, locale)
        // Verify that formatter can format a LocalDate without throwing UnsupportedTemporalTypeException
        LocalDate.now().format(formatter)
        formatter
    }.getOrElse {
        DateTimeFormatter.ofPattern(if (isSameYear && smartYearHiding) "MMMM d" else "MMMM d, yyyy", locale)
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
    smartYearHiding: Boolean = true,
): String {
    if (useRelativeDates) {
        if (date == today) return todayString
        if (date == today.minusDays(1)) return yesterdayString
    }
    return runCatching {
        date.format(if (smartYearHiding && date.year == today.year) sameYearFormatter else otherYearFormatter)
    }.getOrElse {
        date.format(DateTimeFormatter.ofPattern(if (smartYearHiding && date.year == today.year) "MMMM d" else "MMMM d, yyyy", Locale.getDefault()))
    }
}
