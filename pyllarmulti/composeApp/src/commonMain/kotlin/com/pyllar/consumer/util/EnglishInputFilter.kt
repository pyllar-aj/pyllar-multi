package com.pyllar.consumer.util

fun Char.isEnglishLetter(): Boolean = this in 'A'..'Z' || this in 'a'..'z'

fun Char.isEnglishLetterOrDigit(): Boolean = isEnglishLetter() || this in '0'..'9'

fun String.filterEnglishName(uppercase: Boolean = true): String {
    val filtered = filter { it.isEnglishLetter() || it == ' ' }
    return if (uppercase) filtered.uppercase() else filtered
}

fun String.filterEnglishTitleCase(maxLength: Int? = null): String {
    var filtered = filter { it.isEnglishLetter() || it == ' ' }
    if (maxLength != null) filtered = filtered.take(maxLength)
    return filtered.replaceFirstChar { if (it.isEnglishLetter()) it.uppercaseChar() else it }
}

fun String.filterEnglishUppercase(maxLength: Int? = null): String {
    var filtered = filter { it.isEnglishLetter() || it == ' ' }
    if (maxLength != null) filtered = filtered.take(maxLength)
    return filtered.uppercase()
}

fun String.filterEnglishPan(): String {
    val raw = filter { it.isEnglishLetterOrDigit() }.uppercase().take(10)
    return buildString {
        raw.forEachIndexed { index, c ->
            when {
                index < 5 && c.isEnglishLetter() -> append(c)
                index in 5..8 && c.isDigit() -> append(c)
                index == 9 && c.isEnglishLetter() -> append(c)
            }
        }
    }
}

fun String.filterEnglishAddress(maxLength: Int = 32): String {
    val limited = if (length > maxLength) take(maxLength) else this
    val filtered = limited.filter { it.isEnglishLetterOrDigit() || it == ',' || it.isWhitespace() }
    val trimmed = filtered.trimStart()
    if (trimmed.isEmpty()) return ""
    val firstChar = trimmed[0]
    return when {
        firstChar.isEnglishLetter() -> trimmed.replaceFirstChar { it.uppercaseChar() }
        firstChar.isDigit() -> trimmed
        else -> ""
    }
}
