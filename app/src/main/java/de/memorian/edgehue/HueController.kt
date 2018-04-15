package de.memorian.edgehue

import android.content.Context
import com.philips.lighting.hue.sdk.wrapper.HueLog
import com.philips.lighting.hue.sdk.wrapper.Persistence
import com.philips.lighting.hue.sdk.wrapper.knownbridges.KnownBridges

private const val HUE_SDK_NAME = "huesdk"
private const val STORAGE_DIR_NAME = "EdgeHue"

/**
 * Wrapper around the Hue SDK.
 */
class HueController(private val context: Context) {

    init {
        System.loadLibrary(HUE_SDK_NAME)
        Persistence.setStorageLocation(context.filesDir.absolutePath, STORAGE_DIR_NAME)
        if (BuildConfig.DEBUG) HueLog.setConsoleLogLevel(HueLog.LogLevel.DEBUG)
    }

    val hasBridgeIp: Boolean get() = bridgeIp != null
    private val bridgeIp = getLastUsedBridgeIp()

    /**
     * Use the KnownBridges API to retrieve the last connected bridge.
     *
     * @return Ip address of the last connected bridge, or null.
     */
    private fun getLastUsedBridgeIp(): String? {
        val bridges = KnownBridges.getAll()

        return if (bridges.isEmpty()) {
            null
        } else {
            bridges.maxBy { it.lastConnected }?.ipAddress
        }
    }
}
