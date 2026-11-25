package com.sprillex.restaurantfinder.utils

import java.util.Calendar

/**
 * Pure offline evaluator for OpenStreetMap `opening_hours` strings.
 *
 * Supports:
 * - `24/7`
 * - Closed / off rules (`off`, `closed`, `"temporarily closed"`)
 * - Day ranges (`Mo-Fr`, `Sa-Su`, `Tu-Th`, wrapping ranges like `Su-Th`, `Fr-Mo`)
 * - Comma-separated day lists (`Mo,Tu off`, `Fr,Sa 16:00-23:00`)
 * - Daily time windows (`06:00-21:00`, `06:00-24:00`, `11:00-14:00,17:00-22:00`, `19:00+`)
 * - Overnight spans past midnight (`11:00-02:30`, `Fr-Sa 10:30-01:00`)
 *
 * Returns `null` when [openingHours] is null, blank, or unparseable.
 */
object OpeningHoursParser {
    private data class TimeWindow(val startMinute: Int, val endMinute: Int)

    private val DAY_INDEX = mapOf(
        "mo" to 0,
        "tu" to 1,
        "we" to 2,
        "th" to 3,
        "fr" to 4,
        "sa" to 5,
        "su" to 6
    )

    private val DAY_TOKEN_REGEX = Regex("^(Mo|Tu|We|Th|Fr|Sa|Su)", RegexOption.IGNORE_CASE)
    private val DAY_PREFIX_SPLIT_REGEX = Regex(",\\s*(?=(?:Mo|Tu|We|Th|Fr|Sa|Su)(?:\\b|-|,))", RegexOption.IGNORE_CASE)
    private val TIME_RANGE_REGEX = Regex("^(\\d{1,2}):(\\d{2})\\s*-\\s*(\\d{1,2}):(\\d{2})$")
    private val TIME_PLUS_REGEX = Regex("^(\\d{1,2}):(\\d{2})\\+$")

    fun isOpenNow(
        openingHours: String?,
        calendar: Calendar = Calendar.getInstance()
    ): Boolean? {
        val dayIndex = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val minuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        return isOpenAt(openingHours, dayIndex, minuteOfDay)
    }

    /**
     * Evaluates whether [openingHours] is open at [dayIndex] (`0=Mo .. 6=Su`) and [minuteOfDay] (`0..1439`).
     */
    fun isOpenAt(
        openingHours: String?,
        dayIndex: Int,
        minuteOfDay: Int
    ): Boolean? {
        val raw = openingHours?.trim() ?: return null
        if (raw.isBlank()) return null
        if (dayIndex !in 0..6 || minuteOfDay !in 0..1439) return null

        if (raw.equals("24/7", ignoreCase = true)) return true
        val lower = raw.lowercase()
        if (lower == "closed" || lower == "off" || lower.contains("temporarily closed")) {
            return false
        }

        // Strip trailing comments/labels after || if present? Wait: only handle clean rules or fail gracefully
        val scheduleByDay = Array(7) { mutableListOf<TimeWindow>() }
        var parsedAnyRule = false

        val semicolonSegments = raw.split(';')
        for (semiSeg in semicolonSegments) {
            val trimmedSemi = semiSeg.trim()
            if (trimmedSemi.isEmpty()) continue

            val commaRules = splitCommaSeparatedRules(trimmedSemi)
            for (rule in commaRules) {
                val trimmedRule = rule.trim()
                if (trimmedRule.isEmpty()) continue

                // Skip public/school holiday modifiers when evaluating standard weekday/weekend rules
                if (trimmedRule.startsWith("PH", ignoreCase = true) ||
                    trimmedRule.startsWith("SH", ignoreCase = true)
                ) {
                    continue
                }

                val parsed = parseRule(trimmedRule) ?: return null
                val (days, windows, isClosedRule) = parsed
                parsedAnyRule = true
                for (d in days) {
                    if (isClosedRule) {
                        scheduleByDay[d].clear()
                    } else {
                        scheduleByDay[d].clear()
                        scheduleByDay[d].addAll(windows)
                    }
                }
            }
        }

        if (!parsedAnyRule) return null

        // Check today's windows
        for (w in scheduleByDay[dayIndex]) {
            if (w.startMinute == 0 && w.endMinute == 1440) return true
            if (w.startMinute < w.endMinute) {
                if (minuteOfDay in w.startMinute until w.endMinute) return true
            } else if (w.startMinute > w.endMinute) {
                // Overnight window starting today
                if (minuteOfDay >= w.startMinute) return true
            }
        }

        // Check yesterday's overnight spillover into early morning today
        val prevDay = (dayIndex + 6) % 7
        for (w in scheduleByDay[prevDay]) {
            if (w.startMinute > w.endMinute && minuteOfDay < w.endMinute) {
                return true
            }
        }

        return false
    }

    private fun splitCommaSeparatedRules(segment: String): List<String> {
        // Only split on ", <Day>" when a time/closed token precedes the comma
        // e.g. "Su-Th 11:30-20:00, Fr-Sa 11:30-21:00" -> 2 rules
        // whereas "Mo,Tu off" or "Fr,Sa 16:00-23:00" stays 1 rule
        val result = mutableListOf<String>()
        var lastStart = 0
        for (match in DAY_PREFIX_SPLIT_REGEX.findAll(segment)) {
            val prefix = segment.substring(lastStart, match.range.first).trim()
            val hasTimeOrStatus = prefix.contains(':') ||
                prefix.contains("off", ignoreCase = true) ||
                prefix.contains("closed", ignoreCase = true) ||
                prefix.contains("24/7", ignoreCase = true)
            if (hasTimeOrStatus) {
                result.add(prefix)
                lastStart = match.range.last + 1
            }
        }
        result.add(segment.substring(lastStart))
        return result
    }

    private data class ParsedRule(
        val days: List<Int>,
        val windows: List<TimeWindow>,
        val isClosed: Boolean
    )

    private fun parseRule(rule: String): ParsedRule? {
        val trimmed = rule.trim()
        val hasDayPrefix = DAY_TOKEN_REGEX.containsMatchIn(trimmed)
        val days: List<Int>
        val rest: String

        if (hasDayPrefix) {
            val firstSpaceIdx = trimmed.indexOfFirst { it.isWhitespace() }
            if (firstSpaceIdx == -1) return null
            val daySpec = trimmed.substring(0, firstSpaceIdx).trim()
            days = parseDaySpec(daySpec) ?: return null
            rest = trimmed.substring(firstSpaceIdx).trim()
        } else {
            days = (0..6).toList()
            rest = trimmed
        }

        if (rest.equals("off", ignoreCase = true) || rest.equals("closed", ignoreCase = true)) {
            return ParsedRule(days = days, windows = emptyList(), isClosed = true)
        }

        if (rest.equals("24/7", ignoreCase = true)) {
            return ParsedRule(days = days, windows = listOf(TimeWindow(0, 1440)), isClosed = false)
        }

        val windowStrings = rest.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (windowStrings.isEmpty()) return null

        val windows = mutableListOf<TimeWindow>()
        for (ws in windowStrings) {
            val w = parseTimeWindow(ws) ?: return null
            windows.add(w)
        }

        return ParsedRule(days = days, windows = windows, isClosed = false)
    }

    private fun parseDaySpec(daySpec: String): List<Int>? {
        val parts = daySpec.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return null

        val result = mutableListOf<Int>()
        for (part in parts) {
            if (part.contains('-')) {
                val bounds = part.split('-')
                if (bounds.size != 2) return null
                val startDay = DAY_INDEX[bounds[0].trim().lowercase()] ?: return null
                val endDay = DAY_INDEX[bounds[1].trim().lowercase()] ?: return null
                var cur = startDay
                while (true) {
                    if (!result.contains(cur)) result.add(cur)
                    if (cur == endDay) break
                    cur = (cur + 1) % 7
                }
            } else {
                val day = DAY_INDEX[part.lowercase()] ?: return null
                if (!result.contains(day)) result.add(day)
            }
        }
        return result
    }

    private fun parseTimeWindow(spec: String): TimeWindow? {
        val clean = spec.trim()
        TIME_RANGE_REGEX.matchEntire(clean)?.let { match ->
            val sh = match.groupValues[1].toIntOrNull() ?: return null
            val sm = match.groupValues[2].toIntOrNull() ?: return null
            val eh = match.groupValues[3].toIntOrNull() ?: return null
            val em = match.groupValues[4].toIntOrNull() ?: return null

            if (sh !in 0..23 || sm !in 0..59) return null
            if (eh !in 0..24 || em !in 0..59) return null
            if (eh == 24 && em != 0) return null

            val startMin = sh * 60 + sm
            val endMin = eh * 60 + em
            if (startMin == endMin) {
                return TimeWindow(0, 1440)
            }
            return TimeWindow(startMin, endMin)
        }

        TIME_PLUS_REGEX.matchEntire(clean)?.let { match ->
            val sh = match.groupValues[1].toIntOrNull() ?: return null
            val sm = match.groupValues[2].toIntOrNull() ?: return null
            if (sh !in 0..23 || sm !in 0..59) return null
            return TimeWindow(sh * 60 + sm, 1440)
        }

        return null
    }
}
