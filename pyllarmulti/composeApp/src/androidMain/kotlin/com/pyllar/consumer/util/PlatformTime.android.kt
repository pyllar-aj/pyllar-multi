package com.pyllar.consumer.util

import java.util.Calendar

actual fun currentYearMonth(): Pair<Int, Int> {
    val cal = Calendar.getInstance()
    return Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
}
