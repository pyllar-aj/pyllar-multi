package com.pyllar.consumer.util

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitDay

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun getCurrentYear(): Int {
    val calendar = NSCalendar.currentCalendar
    val components = calendar.components(NSCalendarUnitYear, NSDate())
    return components.year.toInt()
}

actual fun getCurrentMonth(): Int {
    val calendar = NSCalendar.currentCalendar
    val components = calendar.components(NSCalendarUnitMonth, NSDate())
    return components.month.toInt()
}

actual fun getCurrentDay(): Int {
    val calendar = NSCalendar.currentCalendar
    val components = calendar.components(NSCalendarUnitDay, NSDate())
    return components.day.toInt()
}

actual fun formatCurrentDateV2(): String {
    val formatter = platform.Foundation.NSDateFormatter().apply {
        dateFormat = "dd MMM HH:mm a"
    }
    return formatter.stringFromDate(NSDate())
}

actual fun formatProcessingDateV2(daysToAdd: Int): String {
    val calendar = NSCalendar.currentCalendar
    val targetDate = calendar.dateByAddingUnit(
        unit = NSCalendarUnitDay,
        value = daysToAdd.toLong(),
        toDate = NSDate(),
        options = 0.toULong()
    ) ?: NSDate()
    val formatter = platform.Foundation.NSDateFormatter().apply {
        dateFormat = "dd MMM yyyy, EEE"
    }
    return formatter.stringFromDate(targetDate)
}

