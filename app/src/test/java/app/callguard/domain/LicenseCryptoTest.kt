package app.callguard.domain

import app.callguard.license.LicenseConfig
import app.callguard.license.LicenseCrypto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LicenseCryptoTest {
    private val id = "A1B2-C3D4-E5F6-7890"

    @Test fun codeWorksForItsOwnDevice() {
        val (pub, priv) = LicenseCrypto.generateKeyPair()
        val code = LicenseCrypto.sign(priv, id)
        assertTrue(LicenseCrypto.verify(pub, id, code))
    }

    @Test fun codeFailsOnAnotherDevice() {
        val (pub, priv) = LicenseCrypto.generateKeyPair()
        val code = LicenseCrypto.sign(priv, id)
        assertFalse(LicenseCrypto.verify(pub, "0000-1111-2222-3333", code))
    }

    @Test fun codeFromDifferentKeyFails() {
        val (pub, _) = LicenseCrypto.generateKeyPair()
        val (_, otherPriv) = LicenseCrypto.generateKeyPair()
        assertFalse(LicenseCrypto.verify(pub, id, LicenseCrypto.sign(otherPriv, id)))
    }

    @Test fun tamperedOrEmptyCodeFails() {
        val (pub, priv) = LicenseCrypto.generateKeyPair()
        val code = LicenseCrypto.sign(priv, id)
        val bad = code.dropLast(2) + (if (code.endsWith("AA")) "BB" else "AA")
        assertFalse(LicenseCrypto.verify(pub, id, bad))
        assertFalse(LicenseCrypto.verify(pub, id, ""))
        assertFalse(LicenseCrypto.verify(pub, id, "not a code"))
    }

    @Test fun idFormattingDoesNotMatter() {
        val (pub, priv) = LicenseCrypto.generateKeyPair()
        val code = LicenseCrypto.sign(priv, "a1b2c3d4e5f67890")
        assertTrue(LicenseCrypto.verify(pub, "A1B2-C3D4-E5F6-7890", code))
        assertTrue(LicenseCrypto.verify(pub, "a1b2 c3d4 e5f6 7890", code))
    }

    @Test fun codeSurvivesWhatsAppLineBreaks() {
        val (pub, priv) = LicenseCrypto.generateKeyPair()
        val code = LicenseCrypto.sign(priv, id)
        val broken = code.chunked(30).joinToString("\n") + "  "
        assertTrue(LicenseCrypto.verify(pub, id, broken))
    }

    @Test fun idValidation() {
        assertTrue(LicenseCrypto.isValidId("a1b2-c3d4-e5f6-7890"))
        assertFalse(LicenseCrypto.isValidId("A1B2-C3D4"))
        assertFalse(LicenseCrypto.isValidId("ZZZZ-C3D4-E5F6-7890"))
        assertEquals("A1B2-C3D4-E5F6-7890", LicenseCrypto.formatId("a1b2c3d4e5f67890"))
    }

    /** Made outside the app with the real private key; proves the embedded public key is valid. */
    @Test fun embeddedPublicKeyAcceptsKnownVector() {
        assertTrue(
            LicenseCrypto.verify(LicenseConfig.PUBLIC_KEY_B64, "0123-4567-89AB-CDEF", "MEYCIQDflnDmmNfkOA8ZLbvc-Vceto0Dtf3-K-UlLWyOzT9o6AIhANPWivEu6KABTOXY8CYnxtgViZr6O-qsfrSxq0IeUgL4"),
        )
    }
}
