package org.openscanner.app

import android.app.Application

class OpenScannerApplication : Application() {
    val graph: AppGraph by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { AppGraph(this) }
}
