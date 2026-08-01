package org.openscanner.app

import org.openscanner.core.domain.Freshness
import org.openscanner.core.domain.ObservedCongestion
import org.openscanner.core.export.WifiLogField
import org.openscanner.core.export.WifiLogStopReason
import org.openscanner.core.model.PlatformCapabilities
import org.openscanner.core.model.ScannerPhase
import org.openscanner.core.model.SignalSample
import org.openscanner.core.model.WifiBand
import org.openscanner.core.model.WifiChannelGroup

enum class AppTab(val label: String) {
    SCAN("Scan"),
    TRACK("Track"),
    SPECTRUM("Spectrum"),
    TOOLS("Tools"),
    SETTINGS("Settings"),
}

data class NetworkUiModel(
    val uiId: String,
    val name: String,
    val bssid: String,
    val band: WifiBand,
    val channelGroup: WifiChannelGroup,
    val channel: Int?,
    val frequencyMhz: Int,
    val footprintCenterFrequencyMhz: Int?,
    val channelWidthMhz: Int?,
    val signalDbm: Int,
    val security: String,
    val generation: String?,
    val connected: Boolean,
    val selected: Boolean,
)

data class ConnectionUiModel(
    val connected: Boolean = false,
    val networkName: String? = null,
    val bssid: String? = null,
    val validated: Boolean? = null,
    val captivePortal: Boolean? = null,
    val linkSpeedMbps: Int? = null,
    val rxLinkSpeedMbps: Int? = null,
    val txLinkSpeedMbps: Int? = null,
    val ipAddress: String? = null,
    val gateway: String? = null,
    val dnsServers: List<String> = emptyList(),
)

data class CongestionUiModel(
    val level: ObservedCongestion = ObservedCongestion.LOW,
    val overlappingNetworks: Int = 0,
    val coChannelNetworks: Int = 0,
    val strongestInterfererDbm: Int? = null,
)

data class NeighborhoodPostureUiModel(
    val accessPointCount: Int = 0,
    val channelGroupCounts: List<Pair<String, Int>> = emptyList(),
    val securityCounts: List<Pair<String, Int>> = emptyList(),
    val generationCounts: List<Pair<String, Int>> = emptyList(),
)

data class StabilityUiModel(
    val label: String = "Insufficient history",
    val assessedSnapshots: Int = 0,
    val presentSnapshots: Int = 0,
    val absentPercent: Int? = null,
    val rssiRangeDb: Int? = null,
    val sufficient: Boolean = false,
)

data class ChannelGroupUiModel(
    val group: WifiChannelGroup,
    val enabled: Boolean,
    val unavailableReason: String? = null,
)

data class WifiLoggingUiState(
    val active: Boolean = false,
    val selectedFields: Set<WifiLogField> = WifiLogField.entries.toSet(),
    val recordedFields: Set<WifiLogField> = emptySet(),
    val recordCount: Int = 0,
    val networkRowCount: Int = 0,
    val durationMs: Long = 0L,
    val stopReason: WifiLogStopReason? = null,
) {
    val hasSession: Boolean get() = recordCount > 0
    val canStart: Boolean get() = selectedFields.isNotEmpty()
    val canExport: Boolean get() = hasSession && !active
}

data class OpenScannerUiState(
    val phase: ScannerPhase = ScannerPhase.CHECKING,
    val activeTab: AppTab = AppTab.TRACK,
    val capabilities: PlatformCapabilities = PlatformCapabilities(true, true, false),
    val channelGroups: List<ChannelGroupUiModel> = emptyList(),
    val selectedChannelGroup: WifiChannelGroup = WifiChannelGroup.GHZ_5_2,
    val networks: List<NetworkUiModel> = emptyList(),
    val selectedNetwork: NetworkUiModel? = null,
    val signalHistory: List<SignalSample> = emptyList(),
    val selectedStability: StabilityUiModel = StabilityUiModel(),
    val historyNowElapsedMs: Long = 0L,
    val connection: ConnectionUiModel = ConnectionUiModel(),
    val congestion: CongestionUiModel = CongestionUiModel(),
    val neighborhoodPosture: NeighborhoodPostureUiModel = NeighborhoodPostureUiModel(),
    val privacyMode: Boolean = false,
    val redactExports: Boolean = true,
    val refreshIntervalSeconds: Int = 30,
    val snapshotSequence: Long? = null,
    val ageMs: Long? = null,
    val freshness: Freshness? = null,
    val likelyThrottled: Boolean = false,
    val safeErrorCode: String? = null,
    val logging: WifiLoggingUiState = WifiLoggingUiState(),
) {
    val paused: Boolean get() = phase == ScannerPhase.PAUSED
}
