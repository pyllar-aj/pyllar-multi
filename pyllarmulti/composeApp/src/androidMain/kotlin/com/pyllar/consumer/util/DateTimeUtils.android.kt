package com.pyllar.consumer.util

import java.util.Calendar

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun getCurrentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)
actual fun getCurrentMonth(): Int = Calendar.getInstance().get(Calendar.MONTH) + 1
actual fun getCurrentDay(): Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
