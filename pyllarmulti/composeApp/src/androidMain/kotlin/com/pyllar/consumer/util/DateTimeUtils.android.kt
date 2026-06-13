package com.pyllar.consumer.util

import java.util.Calendar

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun getCurrentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)
actual fun getCurrentMonth(): Int = Calendar.getInstance().get(Calendar.MONTH) + 1
actual fun getCurrentDay(): Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

actual fun formatCurrentDateV2(): String {
    return java.text.SimpleDateFormat("dd MMM HH:mm a", java.util.Locale.getDefault()).format(java.util.Date())
}

actual fun formatProcessingDateV2(daysToAdd: Int): String {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
    return java.text.SimpleDateFormat("dd MMM yyyy, EEE", java.util.Locale.getDefault()).format(calendar.time)
}

