package org.openscanner.core.domain

import org.openscanner.core.model.SecurityType

object SecurityParser {
    fun fromCapabilities(capabilities: String?): Set<SecurityType> {
        val normalized = capabilities.orEmpty().uppercase()
        if (normalized.isBlank()) return setOf(SecurityType.OPEN)

        val result = linkedSetOf<SecurityType>()
        when {
            "OWE" in normalized -> result += SecurityType.OWE
            "WEP" in normalized -> result += SecurityType.WEP
        }

        val hasSae = "SAE" in normalized
        val hasPsk = "PSK" in normalized
        when {
            hasSae && hasPsk -> result += SecurityType.WPA2_WPA3_PERSONAL
            hasSae -> result += SecurityType.WPA3_PERSONAL
            hasPsk && "WPA2" in normalized -> result += SecurityType.WPA2_PERSONAL
            hasPsk -> result += SecurityType.WPA_PERSONAL
        }

        when {
            "SUITE_B_192" in normalized || "EAP_WPA3" in normalized ->
                result += SecurityType.WPA3_ENTERPRISE
            "EAP" in normalized -> result += SecurityType.ENTERPRISE
        }

        if ("WAPI" in normalized) result += SecurityType.WAPI
        if (result.isEmpty()) {
            val advertisesSecurity = listOf("WPA", "WEP", "RSN", "SAE", "OWE", "EAP", "WAPI")
                .any { it in normalized }
            return if (advertisesSecurity) setOf(SecurityType.UNKNOWN) else setOf(SecurityType.OPEN)
        }
        return result
    }
}
