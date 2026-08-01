package org.openscanner.core.privacy

import org.openscanner.core.model.AccessPointObservation
import org.openscanner.core.model.ConnectionEvidence

object PrivacyRedactor {
    fun redact(
        observation: AccessPointObservation,
        aliasNumber: Int,
    ): AccessPointObservation = observation.copy(
        id = "network-$aliasNumber",
        ssid = "Network $aliasNumber",
        bssid = maskBssid(observation.bssid),
    )

    fun redact(connection: ConnectionEvidence): ConnectionEvidence = connection.copy(
        bssid = connection.bssid?.let(::maskBssid),
        ssid = connection.ssid?.let { "Connected network" },
        ipAddress = connection.ipAddress?.let(::maskIpAddress),
        gateway = connection.gateway?.let(::maskIpAddress),
        dnsServers = connection.dnsServers.map(::maskIpAddress),
    )

    @Suppress("UNUSED_PARAMETER")
    fun maskBssid(value: String): String = "••:••:••:••:••:••"

    fun maskIpAddress(value: String): String = when {
        ':' in value -> "••••:••••::••••"
        '.' in value -> "•••.•••.•••.•••"
        else -> "•••"
    }
}
