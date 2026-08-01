package org.openscanner.core.domain

enum class SignalQuality(val label: String) {
    EXCELLENT("Excellent"),
    GOOD("Good"),
    FAIR("Fair"),
    WEAK("Weak"),
}

object SignalClassifier {
    fun classify(rssiDbm: Int): SignalQuality = when {
        rssiDbm >= -55 -> SignalQuality.EXCELLENT
        rssiDbm >= -67 -> SignalQuality.GOOD
        rssiDbm >= -75 -> SignalQuality.FAIR
        else -> SignalQuality.WEAK
    }
}
