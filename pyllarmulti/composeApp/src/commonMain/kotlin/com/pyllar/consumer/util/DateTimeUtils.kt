package com.pyllar.consumer.util

expect fun currentTimeMillis(): Long

expect fun getCurrentYear(): Int

expect fun getCurrentMonth(): Int

expect fun getCurrentDay(): Int

expect fun formatCurrentDateV2(): String

expect fun formatProcessingDateV2(daysToAdd: Int): String
