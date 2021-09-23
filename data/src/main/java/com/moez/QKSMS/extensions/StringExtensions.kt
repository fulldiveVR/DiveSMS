
package com.moez.QKSMS.extensions

import java.text.Normalizer

/**
 * Strip the accents from a string
 */
fun CharSequence.removeAccents(): String = Normalizer.normalize(this, Normalizer.Form.NFKD).replace(Regex("\\p{M}"), "")
