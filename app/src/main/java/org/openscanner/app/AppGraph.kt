package org.openscanner.app

import android.content.Context
import org.openscanner.data.settings.AndroidSettingsRepository
import org.openscanner.data.settings.SettingsRepository
import org.openscanner.data.wifi.AndroidWifiScanRepository
import org.openscanner.data.wifi.WifiScanRepository

class AppGraph(context: Context) {
    val wifiScanRepository: WifiScanRepository = AndroidWifiScanRepository(context)
    val settingsRepository: SettingsRepository = AndroidSettingsRepository(context)
}
