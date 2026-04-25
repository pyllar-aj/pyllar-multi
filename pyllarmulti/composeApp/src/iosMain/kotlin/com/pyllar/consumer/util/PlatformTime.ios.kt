package com.pyllar.consumer.util

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate

actual fun currentYearMonth(): Pair<Int, Int> {
    val cal = NSCalendar.currentCalendar
    val components = cal.components(NSCalendarUnitYear or NSCalendarUnitMonth, fromDate = NSDate())
    return Pair(components.year.toInt(), components.month.toInt())
}
