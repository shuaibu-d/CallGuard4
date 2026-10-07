package app.callguard.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneNumberNormalizerTest {
    private fun n(s: String) = PhoneNumberNormalizer.normalize(s)

    @Test fun localStaysLocal() = assertEquals("02012345678", n("02012345678"))
    @Test fun plusFormat() = assertEquals("02012345678", n("+2342012345678"))
    @Test fun plusWithSpaces() = assertEquals("02012345678", n("+234 20 1234 5678"))
    @Test fun noPlusFormat() = assertEquals("02012345678", n("2342012345678"))
    @Test fun doubleZeroFormat() = assertEquals("02012345678", n("002342012345678"))
    @Test fun extraZeroAfterCountryCode() = assertEquals("02012345678", n("+23402012345678"))
    @Test fun mobile() = assertEquals("08012345678", n("+2348012345678"))
    @Test fun dashesAndBrackets() = assertEquals("02012345678", n("(020) 1234-5678"))
    @Test fun foreignKeepsPlus() = assertEquals("+442012345678", n("+442012345678"))
    @Test fun foreignDoubleZero() = assertEquals("+442012345678", n("0044 20 1234 5678"))
    @Test fun empty() = assertEquals("", n(""))
    @Test fun onlyZeros() = assertEquals("", n("00"))

    @Test fun prefixPatternVariants() {
        assertEquals("020", PhoneNumberNormalizer.normalizePattern("020*", MatchType.PREFIX))
        assertEquals("020", PhoneNumberNormalizer.normalizePattern("+23420", MatchType.PREFIX))
        assertEquals("020", PhoneNumberNormalizer.normalizePattern("23420", MatchType.PREFIX))
    }

    @Test fun mask() {
        assertEquals("020••••1234", PhoneNumberNormalizer.mask("02012341234"))
        assertEquals("Private number", PhoneNumberNormalizer.mask(""))
    }
}
