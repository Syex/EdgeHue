package de.memorian.edgehue

import android.content.Context
import com.philips.lighting.hue.sdk.wrapper.HueLog
import com.philips.lighting.hue.sdk.wrapper.Persistence
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionType
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeStateCacheType
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeStateUpdatedCallback
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeStateUpdatedEvent
import com.philips.lighting.hue.sdk.wrapper.domain.Bridge
import com.philips.lighting.hue.sdk.wrapper.domain.BridgeBuilder
import com.philips.lighting.hue.sdk.wrapper.domain.resource.GroupType
import com.philips.lighting.hue.sdk.wrapper.knownbridges.KnownBridge
import com.philips.lighting.hue.sdk.wrapper.knownbridges.KnownBridges
import de.memorian.edgehue.model.Light
import de.memorian.edgehue.model.Room

private const val HUE_SDK_NAME = "huesdk"
private const val STORAGE_DIR_NAME = "EdgeHue"
private const val HEARTBEAT_PERIOD = 10_000 //ms

/**
 * Wrapper around the Hue SDK.
 */
class HueController(
        private val context: Context,
        private val callback: Callback
) {

    init {
        System.loadLibrary(HUE_SDK_NAME)
        Persistence.setStorageLocation(context.filesDir.absolutePath, STORAGE_DIR_NAME)
        if (BuildConfig.DEBUG) HueLog.setConsoleLogLevel(HueLog.LogLevel.DEBUG)
    }

    val hasBridgeIp: Boolean get() = knownBridges.isNotEmpty()
    private val knownBridges: List<KnownBridge>
        get() = KnownBridges.getAll()
    private val connectedBridges = mutableListOf<Bridge>()

    fun connectToBridges() {
        for (bridge in knownBridges) {
            val newBridge = BridgeBuilder(context.getString(R.string.app_name), android.os.Build.MODEL)
                    .setConnectionType(BridgeConnectionType.LOCAL)
                    .setIpAddress(bridge.ipAddress)
                    .setBridgeId(bridge.uniqueId)
                    .addBridgeStateUpdatedCallback(BridgeUpdateCallback())
                    //.setBridgeConnectionCallback(bridgeConnectionCallback)
                    .build()
            newBridge.getBridgeConnection(BridgeConnectionType.LOCAL).apply {
                connectionOptions.enableFastConnectionMode(newBridge.identifier)
                heartbeatManager.startHeartbeat(BridgeStateCacheType.LIGHTS_AND_GROUPS, HEARTBEAT_PERIOD)
                connect()
            }
            connectedBridges.add(newBridge)
        }

        callback.bridgesUpdated(context)
    }

    private fun getDisplayGroups(): List<Room> {
        val rooms = mutableListOf<Room>()
        for (bridge in connectedBridges) {
            for (group in bridge.bridgeState.groups.filter { it.groupType == GroupType.ROOM }) {
                val roomName = group.name
                val roomEnabled = group.groupState.isAllOn
                val lights = group.lightIds
                        .map { bridge.bridgeState.getLight(it) }
                        .map {
                            Light(
                                    name = it.name,
                                    enabled = it.lightState.isOn,
                                    reachable = it.lightState.isReachable
                            )
                        }
                rooms.add(Room(roomName, roomEnabled, lights))
            }
        }

        return rooms
    }

    private inner class BridgeUpdateCallback : BridgeStateUpdatedCallback() {

        override fun onBridgeStateUpdated(bridge: Bridge, event: BridgeStateUpdatedEvent) {
            when (event) {
                BridgeStateUpdatedEvent.LIGHTS_AND_GROUPS -> callback.bridgesUpdated(context)
            }
        }
    }

    interface Callback {

        fun bridgesUpdated(context: Context)
    }
}
