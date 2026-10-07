package app.callguard.domain

/**
 * Turns the many formats a number can arrive in into one canonical form.
 *
 * Nigerian numbers become national format starting with 0:
 *   +2342012345678, 2342012345678, 002342012345678 -> 02012345678
 * Numbers from other countries keep an international form: +442012345678
 */
object PhoneNumberNormalizer {
    private const val NG = "234"

    fun normalize(raw: String): String {
        val text = raw.trim()
        val digits = text.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        return when {
            text.startsWith("+") -> fromInternational(digits)
            digits.startsWith("00") -> fromInternational(digits.drop(2))
            digits.startsWith(NG) && digits.length >= 12 -> fromInternational(digits)
            else -> digits
        }
    }

    private fun fromInternational(digits: String): String {
        if (digits.isEmpty()) return ""
        return if (digits.startsWith(NG)) {
            "0" + digits.drop(3).removePrefix("0")
        } else {
            "+$digits"
        }
    }

    /** Normalizes what the user typed into a rule so it can be compared with normalized numbers. */
    fun normalizePattern(pattern: String, matchType: MatchType): String {
        val cleaned = pattern.replace("*", "").trim()
        return when (matchType) {
            MatchType.CONTAINS -> cleaned.filter { it.isDigit() }
            MatchType.PREFIX ->
                if (!cleaned.startsWith("+") && cleaned.startsWith(NG)) normalize("+$cleaned") else normalize(cleaned)
            MatchType.EXACT -> normalize(cleaned)
            else -> ""
        }
    }

    /** 02012341234 -> 020••••1234 */
    fun mask(normalized: String): String {
        if (normalized.isEmpty()) return "Private number"
        if (normalized.length <= 7) return normalized
        return normalized.take(3) + "•".repeat(normalized.length - 7) + normalized.takeLast(4)
    }
}
