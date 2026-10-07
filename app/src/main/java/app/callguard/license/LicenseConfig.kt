package app.callguard.license

object LicenseConfig {
    /** Public half of your signing key. Safe to publish. The private half lives only in the Keys app. */
    const val PUBLIC_KEY_B64 =
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEmb5O0P+E3fohY4sy3VQ8bViGfy2HmiTTJLy9bNiLqXwXWls3DbiGP9erkyih4byLiS8SgOhKMLkAJLe4OqL+Ow=="

    /** Your WhatsApp number in international format, digits only (e.g. "2348012345678"). Leave empty to hide the button. */
    const val SELLER_WHATSAPP = ""
}
