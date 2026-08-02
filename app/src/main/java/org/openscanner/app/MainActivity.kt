package org.openscanner.app

import android.Manifest
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.openscanner.app.ui.OpenScannerApp
import org.openscanner.app.ui.theme.OpenScannerTheme
import org.openscanner.core.export.ExportDocument

class MainActivity : AppCompatActivity() {
    private val graph: AppGraph get() = (application as OpenScannerApplication).graph
    private val viewModel: OpenScannerViewModel by viewModels {
        OpenScannerViewModel.Factory(graph.wifiScanRepository, graph.settingsRepository)
    }
    private var permissionRequestAttempted = false

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionRequestAttempted = true
        viewModel.refreshAfterPermissionResult()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cleanupExpiredExports()
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent {
            OpenScannerTheme {
                val state = viewModel.uiState.collectAsStateWithLifecycle().value
                OpenScannerApp(
                    state = state,
                    onTabSelected = viewModel::selectTab,
                    onChannelGroupSelected = viewModel::selectChannelGroup,
                    onSelectNetwork = viewModel::selectNetwork,
                    onRefresh = viewModel::requestScan,
                    onPauseChanged = viewModel::setPaused,
                    onPrivacyChanged = viewModel::setPrivacyMode,
                    onRedactExportsChanged = viewModel::setRedactExports,
                    onRefreshIntervalChanged = viewModel::setRefreshIntervalSeconds,
                    selectedLanguage = AppLocaleController.currentLanguage(),
                    onLanguageSelected = AppLocaleController::setLanguage,
                    onResetSettings = {
                        viewModel.resetSettings()
                        AppLocaleController.setLanguage(AppLanguage.SYSTEM_DEFAULT)
                    },
                    onRequestPermission = ::requestLocationPermission,
                    onOpenWifiSettings = { openSystemSettings(Settings.ACTION_WIFI_SETTINGS) },
                    onOpenLocationSettings = { openSystemSettings(Settings.ACTION_LOCATION_SOURCE_SETTINGS) },
                    onLogFieldChanged = viewModel::setWifiLogFieldEnabled,
                    onSetAllLogFields = viewModel::setAllWifiLogFields,
                    onStartLogging = viewModel::startWifiLogging,
                    onStopLogging = viewModel::stopWifiLogging,
                    onClearLog = viewModel::clearWifiLog,
                    buildSnapshotExport = viewModel::buildSnapshotExport,
                    buildLogExport = viewModel::buildWifiLogExport,
                    shareExport = ::shareExport,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        graph.wifiScanRepository.start()
    }

    override fun onStop() {
        graph.wifiScanRepository.stop()
        super.onStop()
    }

    private fun requestLocationPermission() {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            viewModel.refreshAfterPermissionResult()
            return
        }
        if (permissionRequestAttempted && !shouldShowRequestPermissionRationale(permission)) {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                },
            )
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
            )
        }
    }

    private fun openSystemSettings(action: String) {
        runCatching { startActivity(Intent(action)) }
    }

    private fun shareExport(document: ExportDocument) {
        runCatching {
            val exportDirectory = File(cacheDir, "exports").apply {
                check(exists() || mkdirs()) { "Unable to create export directory" }
            }
            cleanupExpiredExports()
            val exportFile = nextAvailableExportFile(
                exportDirectory,
                safeExportFileName(document.fileName),
            )
            exportFile.writeText(document.content, Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(
                this,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                exportFile,
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = document.mimeType
                putExtra(Intent.EXTRA_SUBJECT, document.shareSubject)
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri(document.title, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(
                Intent.createChooser(intent, getString(R.string.main_share_chooser_title, document.title)),
            )
            lifecycleScope.launch {
                delay(EXPORT_RETENTION_MS)
                runCatching { exportFile.delete() }
            }
        }.onFailure {
            Toast.makeText(this, getString(R.string.main_export_prepare_error), Toast.LENGTH_LONG).show()
        }
    }

    private fun cleanupExpiredExports() {
        val expirationCutoff = System.currentTimeMillis() - EXPORT_RETENTION_MS
        File(cacheDir, "exports").listFiles()
            ?.filter { it.isFile && it.lastModified() < expirationCutoff }
            ?.forEach { runCatching { it.delete() } }
    }

    private companion object {
        const val EXPORT_RETENTION_MS = 60 * 60_000L
    }
}

internal fun safeExportFileName(fileName: String): String {
    val sanitized = fileName
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .trim('.', '_')
        .take(96)
    return sanitized.ifBlank { "open-scanner-export.txt" }
}

internal fun nextAvailableExportFile(directory: File, fileName: String): File {
    val requested = File(directory, fileName)
    if (!requested.exists()) return requested
    val extensionIndex = fileName.lastIndexOf('.').takeIf { it > 0 } ?: fileName.length
    val baseName = fileName.substring(0, extensionIndex)
    val extension = fileName.substring(extensionIndex)
    var suffix = 2
    while (true) {
        val candidate = File(directory, "$baseName-$suffix$extension")
        if (!candidate.exists()) return candidate
        suffix += 1
    }
}
