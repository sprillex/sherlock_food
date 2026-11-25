package com.sprillex.restaurantfinder.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpeningHoursParserTest {

    // dayIndex: 0=Mo, 1=Tu, 2=We, 3=Th, 4=Fr, 5=Sa, 6=Su

    @Test
    fun nullBlankAndMalformedReturnNull() {
        assertNull(OpeningHoursParser.isOpenAt(null, 0, 12 * 60))
        assertNull(OpeningHoursParser.isOpenAt("", 0, 12 * 60))
        assertNull(OpeningHoursParser.isOpenAt("   ", 0, 12 * 60))
        assertNull(OpeningHoursParser.isOpenAt("call for hours", 0, 12 * 60))
        assertNull(OpeningHoursParser.isOpenAt("Mo-Fr 25:00-26:00", 0, 12 * 60))
        assertNull(OpeningHoursParser.isOpenAt("Xx-Yy 09:00-17:00", 0, 12 * 60))
        assertNull(OpeningHoursParser.isOpenAt("PH off", 0, 12 * 60))
    }

    @Test
    fun twentyFourSevenIsAlwaysOpen() {
        assertEquals(true, OpeningHoursParser.isOpenAt("24/7", 0, 0))
        assertEquals(true, OpeningHoursParser.isOpenAt("24/7", 6, 23 * 60 + 59))
    }

    @Test
    fun dailyWindowWithoutDaysAppliesEveryDay() {
        val hours = "06:00-21:00"
        assertFalse(OpeningHoursParser.isOpenAt(hours, 0, 5 * 60 + 59)!!)
        assertTrue(OpeningHoursParser.isOpenAt(hours, 0, 6 * 60)!!)
        assertTrue(OpeningHoursParser.isOpenAt(hours, 3, 15 * 60)!!)
        assertFalse(OpeningHoursParser.isOpenAt(hours, 6, 21 * 60)!!)
    }

    @Test
    fun dayRangesAndClosedDays() {
        val hours = "Mo,Tu off; We-Fr 11:00-22:00; Sa-Su 10:00-20:00"
        assertFalse(OpeningHoursParser.isOpenAt(hours, 0, 13 * 60)!!) // Monday off
        assertFalse(OpeningHoursParser.isOpenAt(hours, 1, 13 * 60)!!) // Tuesday off
        assertTrue(OpeningHoursParser.isOpenAt(hours, 2, 11 * 60)!!)  // Wednesday open
        assertTrue(OpeningHoursParser.isOpenAt(hours, 4, 21 * 60 + 59)!!) // Friday open
        assertFalse(OpeningHoursParser.isOpenAt(hours, 4, 22 * 60)!!) // Friday closed at 22:00
        assertTrue(OpeningHoursParser.isOpenAt(hours, 5, 10 * 60)!!)  // Saturday open
        assertTrue(OpeningHoursParser.isOpenAt(hours, 6, 19 * 60)!!)  // Sunday open
    }

    @Test
    fun wrappingDayRangeSuToThAndOvernightPastMidnight() {
        val hours = "Su-Th 10:30-23:00; Fr-Sa 10:30-01:00"
        // Sunday daytime open
        assertTrue(OpeningHoursParser.isOpenAt(hours, 6, 12 * 60)!!)
        // Monday 00:30 (closed, since Sunday closed at 23:00)
        assertFalse(OpeningHoursParser.isOpenAt(hours, 0, 30)!!)
        // Friday 23:45 (open, Fr-Sa runs until 01:00)
        assertTrue(OpeningHoursParser.isOpenAt(hours, 4, 23 * 60 + 45)!!)
        // Saturday 00:30 (open from Friday's overnight span)
        assertTrue(OpeningHoursParser.isOpenAt(hours, 5, 30)!!)
        // Saturday 01:00 (closed now)
        assertFalse(OpeningHoursParser.isOpenAt(hours, 5, 60)!!)
        // Sunday 00:45 (open from Saturday's overnight span)
        assertTrue(OpeningHoursParser.isOpenAt(hours, 6, 45)!!)
        // Sunday 01:00 (closed until 10:30)
        assertFalse(OpeningHoursParser.isOpenAt(hours, 6, 60)!!)
    }

    @Test
    fun overnightDailyWindowAndCommaSeparatedRules() {
        val dailyOvernight = "11:00-02:30"
        assertTrue(OpeningHoursParser.isOpenAt(dailyOvernight, 2, 23 * 60)!!)
        assertTrue(OpeningHoursParser.isOpenAt(dailyOvernight, 3, 2 * 60 + 29)!!)
        assertFalse(OpeningHoursParser.isOpenAt(dailyOvernight, 3, 2 * 60 + 30)!!)

        val commaRules = "Su-Th 11:30-20:00, Fr-Sa 11:30-21:00; PH off"
        assertTrue(OpeningHoursParser.isOpenAt(commaRules, 0, 12 * 60)!!)
        assertFalse(OpeningHoursParser.isOpenAt(commaRules, 0, 20 * 60 + 30)!!)
        assertTrue(OpeningHoursParser.isOpenAt(commaRules, 4, 20 * 60 + 30)!!)
    }

    @Test
    fun splitDayScheduleAndClosedKeywords() {
        val split = "Mo-Fr 11:00-14:00,17:00-22:00"
        assertTrue(OpeningHoursParser.isOpenAt(split, 1, 12 * 60)!!)
        assertFalse(OpeningHoursParser.isOpenAt(split, 1, 15 * 60)!!)
        assertTrue(OpeningHoursParser.isOpenAt(split, 1, 18 * 60)!!)

        assertFalse(OpeningHoursParser.isOpenAt("\"temporarily closed\"", 2, 12 * 60)!!)
        assertFalse(OpeningHoursParser.isOpenAt("Su closed; Mo-Fr 09:00-17:00", 6, 12 * 60)!!)
    }
}
