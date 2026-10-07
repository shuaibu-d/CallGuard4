package app.callguard.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockingEngineTest {
    private val engine = BlockingEngine()

    private fun rule(
        id: Long, type: MatchType, pattern: String = "",
        action: RuleAction = RuleAction.SILENCE, enabled: Boolean = true,
    ) = BlockingRule(id, "rule$id", pattern, type, action, enabled)

    private fun hits(number: String?, rules: List<BlockingRule>, isPrivate: Boolean = false, allowed: Set<String> = emptySet()) =
        engine.evaluate(number, isPrivate, rules, allowed) is Decision.Apply

    private val lagos = listOf(rule(1, MatchType.PREFIX, "020"))

    @Test fun prefixBlocksLocalAndInternationalForms() {
        assertTrue(hits("02012345678", lagos))
        assertTrue(hits("02098765432", lagos))
        assertTrue(hits("+2342012345678", lagos))
        assertTrue(hits("+234 20 9876 5432", lagos))
    }

    @Test fun prefixAllowsOthers() {
        assertTrue(!hits("08012345678", lagos))
        assertTrue(!hits("+2348012345678", lagos))
    }

    @Test fun prefixPatternInInternationalForm() {
        assertTrue(hits("02012345678", listOf(rule(1, MatchType.PREFIX, "+23420*"))))
    }

    @Test fun exactMatch() {
        val r = listOf(rule(1, MatchType.EXACT, "08012345678"))
        assertTrue(hits("+2348012345678", r))
        assertTrue(!hits("08012345679", r))
    }

    @Test fun containsMatch() {
        val r = listOf(rule(1, MatchType.CONTAINS, "999"))
        assertTrue(hits("08099912345", r))
        assertTrue(!hits("08012345678", r))
    }

    @Test fun internationalRule() {
        val r = listOf(rule(1, MatchType.INTERNATIONAL))
        assertTrue(hits("+442012345678", r))
        assertTrue(!hits("+2348012345678", r))
        assertTrue(!hits("08012345678", r))
    }

    @Test fun privateRule() {
        val r = listOf(rule(1, MatchType.PRIVATE))
        assertTrue(hits(null, r, isPrivate = true))
        assertTrue(hits("", r))
        assertTrue(!hits("08012345678", r))
    }

    @Test fun privateCallsIgnoreNumberRules() {
        assertTrue(!hits(null, lagos, isPrivate = true))
    }

    @Test fun disabledRuleIgnored() {
        assertTrue(!hits("02012345678", listOf(rule(1, MatchType.PREFIX, "020", enabled = false))))
    }

    @Test fun allowListWins() {
        assertTrue(!hits("+2342012345678", lagos, allowed = setOf("02012345678")))
    }

    @Test fun emptyPatternNeverMatches() {
        assertTrue(!hits("02012345678", listOf(rule(1, MatchType.PREFIX, ""))))
    }

    @Test fun exactBeatsPrefix() {
        val rules = listOf(
            rule(1, MatchType.PREFIX, "020", RuleAction.SILENCE),
            rule(2, MatchType.EXACT, "02012345678", RuleAction.BLOCK),
        )
        val d = engine.evaluate("02012345678", false, rules) as Decision.Apply
        assertEquals(2L, d.rule.id)
    }
}
