package app.callguard.domain

/** Pure Kotlin, no Android dependencies, so it is fully unit-testable. */
class BlockingEngine {

    fun evaluate(
        rawNumber: String?,
        isPrivate: Boolean,
        rules: List<BlockingRule>,
        allowed: Set<String> = emptySet(),
    ): Decision {
        val number = PhoneNumberNormalizer.normalize(rawNumber.orEmpty())
        val unknown = isPrivate || number.isEmpty()

        if (!unknown && number in allowed) return Decision.Allow("allowed")

        val match = rules
            .filter { it.enabled }
            .sortedWith(compareBy({ priority(it.matchType) }, { it.id }))
            .firstOrNull { matches(it, number, unknown) }

        return if (match != null) Decision.Apply(match) else Decision.Allow()
    }

    private fun priority(type: MatchType) = when (type) {
        MatchType.EXACT -> 0
        MatchType.PREFIX -> 1
        MatchType.CONTAINS -> 2
        MatchType.INTERNATIONAL -> 3
        MatchType.PRIVATE -> 4
    }

    private fun matches(rule: BlockingRule, number: String, unknown: Boolean): Boolean {
        return when (rule.matchType) {
            MatchType.PRIVATE -> unknown
            MatchType.INTERNATIONAL -> !unknown && number.startsWith("+")
            else -> {
                if (unknown) return false
                val p = PhoneNumberNormalizer.normalizePattern(rule.pattern, rule.matchType)
                if (p.isEmpty()) return false
                when (rule.matchType) {
                    MatchType.EXACT -> number == p
                    MatchType.PREFIX -> number.startsWith(p)
                    else -> number.contains(p)
                }
            }
        }
    }
}
